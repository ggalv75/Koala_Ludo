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
    /** Gal's original Template Method dice logic is preserved. */
    private final DiceThrow regularThrow;
    /** Gal's UltraBoost surprise die is preserved. */
    private final DiceThrow surpriseThrow;
    private final GameClock gameClock;
    private final List<GameListener> listeners = new ArrayList<>();
    private QuestionService questionService;
    private Function<Position, Optional<QuestionLevel>> questionLevelResolver;
    private QuestionMovementRules questionMovementRules;

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
        this.regularThrow = DiceThrow.regularFor(difficulty.multiplierDie(), dice);
        this.surpriseThrow = new SurpriseUltraBoostThrow(dice);
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

    /** Configure questions using the exact answer movement specified in Ludo Project-V2, p. 4. */
    public void configureQuestions(QuestionService service) {
        configureQuestions(service, this::questionLevelFromBoard, QuestionMovementRules.projectRules());
    }

    /** Configure question logic with explicitly approved forward movement rules. */
    public void configureQuestions(QuestionService service, QuestionMovementRules movementRules) {
        configureQuestions(service, this::questionLevelFromBoard, movementRules);
    }

    /** Inject square detection for deterministic tests and future board configurations. */
    void configureQuestions(QuestionService service,
                            Function<Position, Optional<QuestionLevel>> resolver) {
        configureQuestions(service, resolver, QuestionMovementRules.projectRules());
    }

    /** Configure a question resolver and movement rules before the first die roll. */
    void configureQuestions(QuestionService service, Function<Position, Optional<QuestionLevel>> resolver,
                            QuestionMovementRules movementRules) {
        if (questionService != null) throw new IllegalStateException("Questions already configured");
        if (state.phase() != GameState.Phase.WAITING_FOR_ROLL || state.latestRoll() != null) {
            throw new IllegalStateException("Configure questions before the first roll");
        }
        questionService = Objects.requireNonNull(service, "service");
        questionLevelResolver = Objects.requireNonNull(resolver, "resolver");
        questionMovementRules = Objects.requireNonNull(movementRules, "movementRules");
    }

    /** FR-33: resolve Medium-board question squares to their matching difficulty. */
    private Optional<QuestionLevel> questionLevelFromBoard(Position position) {
        if (position == null) return Optional.empty();
        Cell cell = state.board().cellAt(position);
        if (cell == null) return Optional.empty();
        return switch (cell.type()) {
            case QUESTION_EASY -> Optional.of(QuestionLevel.EASY);
            case QUESTION_MEDIUM -> Optional.of(QuestionLevel.MEDIUM);
            case QUESTION_HARD -> Optional.of(QuestionLevel.HARD);
            default -> Optional.empty();
        };
    }

    /** Roll with Gal's Template Method dice and pause safely during a question. */
    public void rollDice() {
        if (state.phase() != GameState.Phase.WAITING_FOR_ROLL) {
            rejectAction(state.phase() == GameState.Phase.WAITING_FOR_QUESTION_ANSWER ||
                    state.phase() == GameState.Phase.WAITING_FOR_QUESTION_EFFECT
                    ? "יש להשלים את השאלה לפני ההטלה הבאה." : "בחר כלי לפני הטלה נוספת.");
            return;
        }
        state.setFeedbackMessage(null);
        Player player = state.currentPlayer();
        DiceThrow diceThrow = state.turnState().takeSpecialRoll() ? surpriseThrow : regularThrow;
        ThrowResult result = diceThrow.perform(state);
        state.setLastRoll(result.roll());
        state.log(player.name() + ": " + diceThrow.describe(result.roll()));
        switch (result.outcome()) {
            case CHOOSE_PIECE -> {
                state.setLegalMoves(result.legalMoves());
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

    /** Apply a normal die move using the same interaction logic as question-induced moves. */
    private void apply(Move move) {
        Player player = state.currentPlayer();
        CommittedMove committed = commitMove(move);
        completeMove(move, player, committed);
    }

    /** Return movement events and the new landing position after all interaction rules run. */
    private record CommittedMove(Piece piece, Position destination,
                                 PieceInteractionEvent interaction, PlayerFinishEvent finish) { }

    /** Commit one validated movement with capture, protection, Double Block and ranking rules. */
    private CommittedMove commitMove(Move move) {
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
        int direction = Integer.compare(move.toStep(), move.fromStep());
        Position blockedBy = move.partial()
                ? state.board().positionOf(piece.color(), move.toStep() + direction) : null;
        PieceInteractionEvent interaction = new PieceInteractionEvent(
                capture, protection, blockedBy, changesToBlocks(before));
        if (state.isFinished(piece)) state.log(player.name() + " הכניס כלי לבית הסיום!");
        return new CommittedMove(piece, to, interaction, rankIfAllPiecesHome(player));
    }

    /** Detect another question, otherwise finish the turn, then publish movement events. */
    private void completeMove(Move move, Player player, CommittedMove committed) {
        Question question = openQuestionForLanding(committed.piece(), committed.destination());
        boolean gameOver = question == null && endTurnAfterMove(player, committed.finish() != null);
        notifyMoved(move, committed.interaction(), committed.finish(), question);
        if (gameOver) notifyGameOver();
    }

    /** Inform observers when a completed move ends the game. */
    private void notifyGameOver() {
        for (GameListener listener : List.copyOf(listeners)) listener.onGameOver(state.result());
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

    /** FR-36/FR-37: validate an answer, publish the result and immediately apply its movement. */
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
        state.setFeedbackMessage(answer.correct() ? "תשובה נכונה!" : "תשובה שגויה.");
        state.log(answer.correct() ? "תשובה נכונה!" : "תשובה שגויה.");
        for (GameListener listener : List.copyOf(listeners)) listener.onQuestionAnswered(answer, pieceId);
        applyQuestionEffect();
        return Optional.of(answer);
    }

    /** FR-37: apply forward or backward movement with the usual blocking and capture rules. */
    private void applyQuestionEffect() {
        QuestionService.AnswerResult answer = state.pendingQuestionResult().orElseThrow();
        Piece piece = state.piece(state.questionPieceId().orElseThrow());
        Player player = state.currentPlayer();
        int currentStep = piece.step();
        int requestedStep = currentStep + questionMovementRules.signedSteps(answer);
        int finish = state.board().finishStep();

        // A question does not bypass the exact-finish rule or move backwards into the castle.
        int targetStep = requestedStep > finish ? currentStep : Math.max(0, requestedStep);
        int reachableStep = MoveValidator.furthestReachable(state, piece, targetStep);
        state.clearQuestion(); // Unblock a subsequent question triggered by the destination.

        if (reachableStep == currentStep) {
            state.log("השאלה הסתיימה ללא שינוי במיקום הכלי.");
            boolean gameOver = endTurnAfterMove(player, false);
            notifyChanged();
            if (gameOver) notifyGameOver();
            return;
        }
        Move effectMove = new Move(piece.id(), currentStep, reachableStep,
                false, reachableStep != targetStep);
        state.log((reachableStep > currentStep ? "התקדמות " : "חזרה ")
                + Math.abs(reachableStep - currentStep) + " צעדים בעקבות השאלה.");
        apply(effectMove);
    }

    /** FR-45: rank the player when all four pieces are home. */
    private PlayerFinishEvent rankIfAllPiecesHome(Player player) {
        if (!RankingManager.rankIfAllPiecesHome(state, player)) return null;
        state.log("מקום " + player.rank() + ": " + player.name());
        return new PlayerFinishEvent(player.index(), player.name(), player.color(), player.rank());
    }

    /** Preserve Gal's special/extra-turn priorities after completing a question or move. */
    private boolean endTurnAfterMove(Player player, boolean justFinished) {
        if (justFinished && RankingManager.isGameOver(state)) {
            endGame();
            return true;
        }
        switch (TurnManager.afterMove(state, player, state.lastRoll())) {
            case SPECIAL_ROLL -> {
                state.setLastRoll(null);
                state.setPhase(GameState.Phase.WAITING_FOR_ROLL);
                state.log("הטלה מיוחדת ל" + player.name() + ": קובייה רגילה + UltraBoost!");
            }
            case ROLL_AGAIN -> {
                state.setLastRoll(null);
                state.setPhase(GameState.Phase.WAITING_FOR_ROLL);
                state.log("תור נוסף ל" + player.name() + "!");
            }
            case PASS_TURN -> passTurn();
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
