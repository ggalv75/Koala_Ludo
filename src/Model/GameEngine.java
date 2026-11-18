package Model;

import java.time.Clock;
import java.util.*;

/**
 * Entry point to the game rules (Facade design pattern) and subject of the Observer pattern.
 * The Controller calls {@link #rollDice()} and {@link #movePiece(int)}; registered
 * {@link GameListener}s (the views) are notified after every change.
 * Invalid actions never throw: they only produce a message for the player (NFR-01).
 */
public final class GameEngine {
    private final GameState state;
    /** Template Method: the level's regular throw (step die only at Easy, plus Boost/UltraBoost above). */
    private final DiceThrow regularThrow;
    /** Template Method: the UltraBoost surprise throw (FR-21), the same at every level. */
    private final DiceThrow surpriseThrow;
    private final GameClock gameClock;
    private final Dice dice;
    private final SurpriseSelector surpriseSelector;
    private final List<Runnable> pendingNotifications = new ArrayList<>();
    private final List<GameListener> listeners = new ArrayList<>();

    /** Initialize a game with validated players and deterministic dependencies. */
    public GameEngine(Difficulty difficulty, List<String> names, List<PlayerColor> colors) {
        this(difficulty, names, colors, new Random(), new Dice(), Clock.systemDefaultZone());
    }

    /** Full constructor; random sources and the clock are injected so tests are repeatable. */
    GameEngine(Difficulty difficulty, List<String> names, List<PlayerColor> colors,
               Random boardRandom, Dice dice, Clock clock) {
        this(BoardFactory.create(difficulty, boardRandom), names, colors,
                dice, clock, new SurpriseSelector());
    }

    /** Inject a configured board and random sources for repeatable integration tests. */
    GameEngine(Board board, List<String> names, List<PlayerColor> colors,
               Dice dice, Clock clock, SurpriseSelector surpriseSelector) {
        Objects.requireNonNull(board, "board");
        SetupValidator.validate(board.difficulty(), names, colors).ifPresent(message -> {
            throw new IllegalArgumentException(message);
        });
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            players.add(new Player(i, names.get(i).trim(), colors.get(i)));
        }
        this.state = new GameState(board, players);
        this.dice = Objects.requireNonNull(dice, "dice");
        this.surpriseSelector = Objects.requireNonNull(surpriseSelector, "surpriseSelector");
        this.regularThrow = DiceThrow.regularFor(board.difficulty().multiplierDie(), dice);
        this.surpriseThrow = new SurpriseUltraBoostThrow(dice);
        this.gameClock = new GameClock(Objects.requireNonNull(clock, "clock"));
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

    /**
     * FR-15 to FR-21: rolls the dice and works out what the player may do. The steps of the
     * throw are fixed in {@link DiceThrow#perform} (Template Method); here we only choose which
     * throw it is and react to the outcome.
     */
    public void rollDice() {
        if (state.phase() != GameState.Phase.WAITING_FOR_ROLL) {
            rejectAction("בחר כלי לפני הטלה נוספת.");
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
        if (state.phase() == GameState.Phase.WAITING_FOR_SURPRISE_CHOICE) {
            return selectSurpriseOpponentPiece(pieceId);
        }
        if (state.phase() != GameState.Phase.WAITING_FOR_MOVE) {
            rejectAction("הטל קובייה לפני בחירת כלי.");
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

    /** Accept only an opponent move offered by the pending surprise. */
    public boolean selectSurpriseOpponentPiece(int pieceId) {
        if (state.phase() != GameState.Phase.WAITING_FOR_SURPRISE_CHOICE) {
            rejectAction("אין כרגע בחירת כלי יריב.");
            return false;
        }
        Optional<Move> move = state.legalMoves().stream()
                .filter(candidate -> candidate.pieceId() == pieceId).findFirst();
        if (move.isEmpty()) {
            rejectAction("בחר כלי יריב מסומן.");
            return false;
        }
        int roll = dice.roll().value();
        int steps = (roll + 1) / 2;
        Optional<Move> actualMove = SurpriseMovement.backward(state, pieceId, steps);
        if (actualMove.isEmpty()) {
            rejectAction("לא ניתן להזיז את הכלי שנבחר.");
            return false;
        }
        state.log("קוביית ההפתעה: " + roll + " — תנועה לאחור: " + steps);
        state.setFeedbackMessage(null);
        apply(actualMove.get());
        return true;
    }

    /** Resolve movements and chained surprises before completing the active turn. */
    private void apply(Move firstMove) {
        Player activePlayer = state.currentPlayer();
        state.setPhase(GameState.Phase.RESOLVING_EFFECTS);
        Move nextMove = firstMove;

        // Iteration avoids growing the Java call stack for chained surprises.
        while (nextMove != null) {
            Move move = nextMove;
            MovementOutcome outcome = commitMovement(move);
            pendingNotifications.add(() -> publishMovement(move, outcome));
            Piece piece = state.piece(move.pieceId());

            if (RankingManager.isGameOver(state)) {
                endGame();
                flushNotifications();
                return;
            }

            Cell landing = state.board().cellAt(piece.color(), piece.step());
            if (landing == null || landing.type() != CellType.SURPRISE) break;

            SurpriseType type = surpriseSelector.draw();
            SurpriseEvent event = new SurpriseEvent(
                    activePlayer.index(), piece.id(), type);
            state.log("הפתעה: " + type.displayName());
            pendingNotifications.add(() -> {
                for (GameListener listener : List.copyOf(listeners)) {
                    listener.onSurpriseDrawn(event);
                }
            });

            nextMove = switch (type) {
                case DIRECT_FINISH ->
                        SurpriseMovement.directFinish(state, piece.id()).orElse(null);
                case RETURN_TO_CASTLE ->
                        SurpriseMovement.returnToCastle(state, piece.id()).orElse(null);
                case MOVE_BACKWARD_THREE ->
                        SurpriseMovement.backward(state, piece.id(), 3).orElse(null);
                case LOSE_NEXT_TURN -> {
                    TurnManager.loseNextTurn(state, activePlayer);
                    yield null;
                }
                case ULTRA_BOOST_ROLL -> {
                    TurnManager.grantSpecialRoll(state);
                    yield null;
                }
                case MOVE_OPPONENT_BACKWARD -> {
                    offerOpponentMoves(activePlayer);
                    yield null;
                }
            };

            if (state.phase() == GameState.Phase.WAITING_FOR_SURPRISE_CHOICE) {
                flushNotifications();
                return;
            }
        }

        endTurnAfterMove(activePlayer, activePlayer.isFinished());
        flushNotifications();
    }

    /** Offer movable opposing pawns; the ordinary die is rolled only after selection. */
    private void offerOpponentMoves(Player activePlayer) {
        List<Move> options = new ArrayList<>();
        for (Piece candidate : state.pieces()) {
            if (candidate.ownerIndex() == activePlayer.index()) continue;
            if (state.players().get(candidate.ownerIndex()).isFinished()) continue;
            SurpriseMovement.backward(state, candidate.id(), 1).ifPresent(options::add);
        }
        if (options.isEmpty()) {
            state.log("ההפתעה בוטלה: אין כלי יריב שניתן להזיז.");
            return;
        }
        state.setLegalMoves(options);
        state.setPhase(GameState.Phase.WAITING_FOR_SURPRISE_CHOICE);
        state.setFeedbackMessage("בחר כלי יריב מסומן להזזה לאחור.");
    }

    /** Publish the committed movement before the next surprise in the event sequence. */
    private void publishMovement(Move move, MovementOutcome outcome) {
        for (GameListener listener : List.copyOf(listeners)) listener.onPieceMoved(move);
        if (outcome.interaction().hasVisualEffect()) {
            for (GameListener listener : List.copyOf(listeners)) {
                listener.onPieceInteraction(outcome.interaction());
            }
        }
        if (outcome.playerFinish() != null) {
            for (GameListener listener : List.copyOf(listeners)) {
                listener.onPlayerFinished(outcome.playerFinish());
            }
        }
    }

    /** Refresh observers only after reaching a stable state or awaiting a choice. */
    private void flushNotifications() {
        List<Runnable> notifications = List.copyOf(pendingNotifications);
        pendingNotifications.clear();
        for (Runnable notification : notifications) notification.run();
        notifyChanged();
        if (state.phase() == GameState.Phase.GAME_OVER) {
            for (GameListener listener : List.copyOf(listeners)) {
                listener.onGameOver(state.result());
            }
        }
    }

    /**
     * Commit one already validated movement and calculate its events.
     * Does not pass the turn or publish notifications: landing effects must
     * finish before the engine decides who plays next.
     */
    private MovementOutcome commitMovement(Move move) {
        Piece piece = state.piece(move.pieceId());
        Player owner = state.players().get(piece.ownerIndex());
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
            state.log(owner.name() + " אכל כלי של "
                    + state.players().get(victim.ownerIndex()).name() + "!");
        }

        Position protection =
                InteractionRules.sharesProtectedSquareWithOpponent(state, piece)
                        ? to : null;

        Position blockedBy = null;
        if (move.partial()) {
            int direction = Integer.compare(move.toStep(), move.fromStep());
            blockedBy = state.board().positionOf(
                    piece.color(), move.toStep() + direction);
        }

        PieceInteractionEvent interaction = new PieceInteractionEvent(
                capture, protection, blockedBy, changesToBlocks(before));

        if (state.isFinished(piece)) {
            state.log(owner.name() + " הכניס כלי לבית הסיום!");
        }

        PlayerFinishEvent playerFinish = rankIfAllPiecesHome(owner);
        return new MovementOutcome(interaction, playerFinish);
    }

    /** Movement events retained until the engine is ready to publish them. */
    private record MovementOutcome(
            PieceInteractionEvent interaction,
            PlayerFinishEvent playerFinish) { }

    /** FR-45: ranks the player when all four pieces are home. Returns the finish event, or null. */
    private PlayerFinishEvent rankIfAllPiecesHome(Player player) {
        if (!RankingManager.rankIfAllPiecesHome(state, player)) return null;
        state.log("מקום " + player.rank() + ": " + player.name());
        return new PlayerFinishEvent(player.index(), player.name(), player.color(), player.rank());
    }

    /**
     * FR-47, FR-19, FR-21, FR-22, FR-46: ends the game, gives the UltraBoost surprise throw,
     * gives another roll after a six (not to a player who has just finished or lost the turn),
     * or passes the turn. Returns true if the game ended.
     */
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

    /** FR-47, FR-48: stops the clock, ranks the last player and locks the game. */
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
    private void passTurn() {
        TurnManager.passTurn(state);
    }

    /** Notify every registered observer of the current authoritative state. */
    private void notifyChanged() {
        for (GameListener listener : List.copyOf(listeners)) listener.onStateChanged(state);
    }
}
