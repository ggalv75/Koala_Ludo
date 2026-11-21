package Model;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;

/**
 * Entry point to the game rules (Facade design pattern) and subject of the Observer pattern.
 * The Controller calls game actions; registered GameListeners observe committed changes.
 * Invalid player actions produce feedback instead of terminating the game (NFR-01).
 */
public final class GameEngine {
    private final GameState state;
    private final Dice dice;
    private final GameClock gameClock;
    private final List<GameListener> listeners = new ArrayList<>();
    private QuestionService questionService;
    private Function<Position, Optional<QuestionLevel>> questionLevelResolver;

    /** Initialize a game with validated players and deterministic dependencies. */
    public GameEngine(Difficulty difficulty, List<String> names, List<PlayerColor> colors) {
        this(difficulty, names, colors, new Random(), new Dice(), Clock.systemDefaultZone());
    }

    /** Full constructor; random sources and the clock are injected so tests are repeatable. */
    GameEngine(Difficulty difficulty, List<String> names, List<PlayerColor> colors,
               Random boardRandom, Dice dice, Clock clock) {
        SetupValidator.validate(difficulty, names, colors).ifPresent(message -> {
            throw new IllegalArgumentException(message);
        });
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            players.add(new Player(i, names.get(i).trim(), colors.get(i)));
        }
        this.state = new GameState(BoardFactory.create(difficulty, boardRandom), players);
        this.dice = Objects.requireNonNull(dice);
        this.gameClock = new GameClock(clock);
        state.log("המשחק התחיל!");
    }

    /** Return the current authoritative game state. */
    public GameState state() { return state; }

    /** Game time for the clock on the board (FR-48). */
    public long elapsedSeconds() { return gameClock.elapsedSeconds(); }

    /** Register one observer for game state changes. */
    public void addListener(GameListener listener) { listeners.add(Objects.requireNonNull(listener)); }

    /** Unregister a previously added observer. */
    public void removeListener(GameListener listener) { listeners.remove(listener); }

    /** Configure question logic before the first turn; no View or encryption key belongs in this class. */
    public void configureQuestions(QuestionService service) {
        configureQuestions(service, this::questionLevelFromBoard);
    }

    /** Inject square detection for deterministic tests and future board configurations. */
    void configureQuestions(QuestionService service,
                            Function<Position, Optional<QuestionLevel>> resolver) {
        if (questionService != null) throw new IllegalStateException("Questions already configured");
        if (state.phase() != GameState.Phase.WAITING_FOR_ROLL || state.latestRoll() != null) {
            throw new IllegalStateException("Configure questions before the first roll");
        }
        questionService = Objects.requireNonNull(service, "service");
        questionLevelResolver = Objects.requireNonNull(resolver, "resolver");
    }

    /** FR-33: translate question cells into their three difficulty levels when Omer adds them. */
    private Optional<QuestionLevel> questionLevelFromBoard(Position position) {
        if (position == null) return Optional.empty();
        Cell cell = state.board().cellAt(position);
        if (cell == null) return Optional.empty();
        // Bridge to Omer's future CellType values; Easy's existing enum does not define them yet.
        return switch (cell.type().name()) {
            case "QUESTION_EASY" -> Optional.of(QuestionLevel.EASY);
            case "QUESTION_MEDIUM" -> Optional.of(QuestionLevel.MEDIUM);
            case "QUESTION_HARD" -> Optional.of(QuestionLevel.HARD);
            default -> Optional.empty();
        };
    }

    /** FR-15, FR-18, FR-19, FR-20: roll the die and work out what the player may do. */
    public void rollDice() {
        if (state.phase() != GameState.Phase.WAITING_FOR_ROLL) {
            rejectAction(state.phase() == GameState.Phase.WAITING_FOR_QUESTION_ANSWER ||
                    state.phase() == GameState.Phase.WAITING_FOR_QUESTION_EFFECT
                    ? "יש להשלים את השאלה לפני ההטלה הבאה." : "בחר כלי לפני הטלה נוספת.");
            return;
        }
        state.setFeedbackMessage(null);
        Player player = state.currentPlayer();
        DiceRoll roll = dice.roll();
        state.setLastRoll(roll);
        state.log(player.name() + ": תוצאת הקובייה - " + roll.value());
        List<Move> moves = MoveValidator.legalMoves(state, roll);
        switch (TurnManager.afterRoll(state, roll, !moves.isEmpty())) {
            case CHOOSE_PIECE -> {
                state.setLegalMoves(moves);
                state.setPhase(GameState.Phase.WAITING_FOR_MOVE);
            }
            case ROLL_AGAIN -> {
                state.log(player.name() + ": אין מהלך חוקי");
                state.log("תור נוסף ל" + player.name() + "!");
                state.setLastRoll(null);
            }
            case PASS_TURN -> {
                state.log(player.name() + ": אין מהלך חוקי");
                passTurn();
            }
            case THIRD_SIX_CANCELLED -> {
                state.log(player.name() + ": שלוש שישיות ברצף - התור בוטל!");
                passTurn();
            }
        }
        notifyChanged();
    }

    /** NFR-01: an action that is not allowed now only shows a message to the player. */
    private void rejectAction(String hint) {
        state.setFeedbackMessage(state.phase() == GameState.Phase.GAME_OVER ? "המשחק הסתיים." : hint);
        notifyChanged();
    }

    /** Reject illegal selections or commit exactly one approved move. */
    public boolean movePiece(int pieceId) {
        if (state.phase() != GameState.Phase.WAITING_FOR_MOVE) {
            rejectAction(state.phase() == GameState.Phase.WAITING_FOR_QUESTION_ANSWER ||
                    state.phase() == GameState.Phase.WAITING_FOR_QUESTION_EFFECT
                    ? "יש להשלים את השאלה לפני בחירת כלי נוסף." : "הטל קובייה לפני בחירת כלי.");
            return false;
        }
        Optional<Move> move = state.legalMoves().stream().filter(m -> m.pieceId() == pieceId).findFirst();
        if (move.isEmpty()) {
            rejectAction("בחר כלי מסומן אחר.");
            return false;
        }
        state.setFeedbackMessage(null);
        apply(move.get());
        return true;
    }

    /** Apply one validated move and record its interaction effects for the View. */
    private void apply(Move move) {
        Player player = state.currentPlayer();
        Piece piece = state.piece(move.pieceId());
        Position from = state.positionOf(piece);
        Position to = state.board().positionOf(piece.color(), move.toStep());
        Map<Position, Set<Integer>> before = snapshotBlocks(from, to);
        piece.moveTo(move.toStep());
        state.setLegalMoves(List.of());
        PieceInteractionEvent.Capture capture = null;
        Optional<Piece> captured = InteractionRules.capturableOpponent(state, piece);
        if (captured.isPresent()) {
            Piece victim = captured.get();
            capture = new PieceInteractionEvent.Capture(victim.id(), to);
            victim.returnToCastle();
            state.log(player.name() + " אכל כלי של " + state.players().get(victim.ownerIndex()).name() + "!");
        }
        Position protection = InteractionRules.sharesProtectedSquareWithOpponent(state, piece) ? to : null;
        Position blockedBy = move.partial() ? state.board().positionOf(piece.color(), move.toStep() + 1) : null;
        PieceInteractionEvent interaction = new PieceInteractionEvent(capture, protection, blockedBy, changesToBlocks(before));
        if (state.isFinished(piece)) {
            state.log(player.name() + " הכניס כלי לבית הסיום!");
        }
        PlayerFinishEvent playerFinish = rankIfAllPiecesHome(player);

        // Do not end the turn when the move landed on a question square.
        Question question = openQuestionForLanding(piece, to);
        boolean gameOver = question == null && endTurnAfterMove(player, playerFinish != null);
        notifyMoved(move, interaction, playerFinish, question);
        if (gameOver) {
            for (GameListener listener : List.copyOf(listeners)) listener.onGameOver(state.result());
        }
    }

    /** FR-33–FR-35: open one question, remember its piece and pause the current turn. */
    private Question openQuestionForLanding(Piece piece, Position destination) {
        if (questionService == null || destination == null || state.isFinished(piece)) return null;
        Optional<QuestionLevel> level = Objects.requireNonNull(
                questionLevelResolver.apply(destination), "question resolver result");
        if (level.isEmpty()) return null;
        Question question = questionService.startQuestion(level.get());
        state.beginQuestion(question, piece.id());
        state.log("שאלה ברמה " + question.level() + " עבור " + state.currentPlayer().name());
        return question;
    }

    /** FR-36: accept one legal answer and notify observers, leaving movement for stage 6. */
    public Optional<QuestionService.AnswerResult> submitQuestionAnswer(int selectedIndex) {
        if (state.phase() != GameState.Phase.WAITING_FOR_QUESTION_ANSWER || questionService == null) {
            rejectAction("אין שאלה שממתינה לתשובה.");
            return Optional.empty();
        }
        final QuestionService.AnswerResult answer;
        try {
            answer = questionService.submitAnswer(selectedIndex);
        } catch (IllegalArgumentException error) {
            rejectAction("בחר אחת מארבע אפשרויות התשובה.");
            return Optional.empty();
        }
        int pieceId = state.questionPieceId().orElseThrow();
        state.recordQuestionAnswer(answer);
        state.setFeedbackMessage(null);
        state.log(answer.correct() ? "תשובה נכונה!" : "תשובה שגויה.");
        for (GameListener listener : List.copyOf(listeners)) listener.onQuestionAnswered(answer, pieceId);
        notifyChanged();
        // Stage 6 will apply the movement result and only then resume/finish the turn.
        return Optional.of(answer);
    }

    /** FR-45: rank the player when all four pieces are home. */
    private PlayerFinishEvent rankIfAllPiecesHome(Player player) {
        if (!RankingManager.rankIfAllPiecesHome(state, player)) return null;
        state.log("מקום " + player.rank() + ": " + player.name());
        return new PlayerFinishEvent(player.index(), player.name(), player.color(), player.rank());
    }

    /** FR-47, FR-19, FR-46: end or advance the turn after a completed move. */
    private boolean endTurnAfterMove(Player player, boolean justFinished) {
        if (justFinished && RankingManager.isGameOver(state)) {
            endGame();
            return true;
        }
        if (TurnManager.rollsAgain(player, state.lastRoll())) {
            state.setLastRoll(null);
            state.setPhase(GameState.Phase.WAITING_FOR_ROLL);
            state.log("תור נוסף ל" + player.name() + "!");
        } else {
            passTurn();
        }
        return false;
    }

    /** Snapshot all Double Block owners at the squares affected by a move. */
    private Map<Position, Set<Integer>> snapshotBlocks(Position from, Position to) {
        Map<Position, Set<Integer>> snapshot = new LinkedHashMap<>();
        if (from != null) snapshot.put(from, InteractionRules.blockOwners(state, from));
        if (to != null) snapshot.put(to, InteractionRules.blockOwners(state, to));
        return snapshot;
    }

    /** Compare block owners before and after movement to emit exact FR-30/FR-32 changes. */
    private List<PieceInteractionEvent.BlockChange> changesToBlocks(Map<Position, Set<Integer>> before) {
        List<PieceInteractionEvent.BlockChange> changes = new ArrayList<>();
        for (Map.Entry<Position, Set<Integer>> entry : before.entrySet()) {
            Position square = entry.getKey();
            Set<Integer> oldOwners = entry.getValue();
            Set<Integer> newOwners = InteractionRules.blockOwners(state, square);
            for (Integer owner : oldOwners) {
                if (!newOwners.contains(owner)) {
                    changes.add(new PieceInteractionEvent.BlockChange(owner, square, false));
                }
            }
            for (Integer owner : newOwners) {
                if (!oldOwners.contains(owner)) {
                    changes.add(new PieceInteractionEvent.BlockChange(owner, square, true));
                }
            }
        }
        return List.copyOf(changes);
    }

    /** FR-47, FR-48: stop the clock, rank the last player and lock the game. */
    private void endGame() {
        gameClock.stop();
        RankingManager.rankRemainingPlayers(state);
        GameResult result = new GameResult(state.difficulty(), RankingManager.standings(state),
                gameClock.elapsedSeconds(), gameClock.endedAt());
        state.setResult(result);
        state.setLegalMoves(List.of());
        state.setLastRoll(null);
        state.setPhase(GameState.Phase.GAME_OVER);
        state.log("המשחק הסתיים!");
    }

    /** Advance to the next unfinished player. */
    private void passTurn() { TurnManager.passTurn(state); }

    /** Publish movement and question events before refreshing the observers' view of the state. */
    private void notifyMoved(Move move, PieceInteractionEvent interaction,
                             PlayerFinishEvent playerFinish, Question question) {
        for (GameListener listener : List.copyOf(listeners)) listener.onPieceMoved(move);
        if (interaction.hasVisualEffect()) {
            for (GameListener listener : List.copyOf(listeners)) listener.onPieceInteraction(interaction);
        }
        if (playerFinish != null) {
            for (GameListener listener : List.copyOf(listeners)) listener.onPlayerFinished(playerFinish);
        }
        if (question != null) {
            for (GameListener listener : List.copyOf(listeners)) {
                listener.onQuestionRequested(question, move.pieceId());
            }
        }
        notifyChanged();
    }

    /** Notify every registered observer of the current authoritative state. */
    private void notifyChanged() {
        for (GameListener listener : List.copyOf(listeners)) listener.onStateChanged(state);
    }
}
