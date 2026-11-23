package Model;

import java.time.Clock;
import java.util.*;

/**
 * Entry point to the game rules and subject of the Observer pattern.
 * Surprise effects are delegated to interchangeable Strategy implementations.
 * The Controller calls game actions; registered listeners receive events.
 */
public final class GameEngine {

    private final GameState state;

    /** Template Method: the level's regular throw. */
    private final DiceThrow regularThrow;

    /** Template Method: the special UltraBoost surprise throw. */
    private final DiceThrow surpriseThrow;

    private final GameClock gameClock;
    private final Dice dice;
    private final SurpriseSelector surpriseSelector;

    /** Strategy: effect policies are separate from movement and turn sequencing. */
    private final SurpriseStrategies surpriseStrategies;

    private final List<Runnable> pendingNotifications =
            new ArrayList<>();

    private final List<GameListener> listeners =
            new ArrayList<>();

    /** Initialize a game with validated players and deterministic dependencies. */
    public GameEngine(
            Difficulty difficulty,
            List<String> names,
            List<PlayerColor> colors) {

        this(
                difficulty,
                names,
                colors,
                new Random(),
                new Dice(),
                Clock.systemDefaultZone());
    }

    /** Inject random sources and the clock for repeatable tests. */
    GameEngine(
            Difficulty difficulty,
            List<String> names,
            List<PlayerColor> colors,
            Random boardRandom,
            Dice dice,
            Clock clock) {

        this(
                BoardFactory.create(difficulty, boardRandom),
                names,
                colors,
                dice,
                clock,
                new SurpriseSelector());
    }

    /** Inject a configured board and random sources for integration tests. */
    GameEngine(
            Board board,
            List<String> names,
            List<PlayerColor> colors,
            Dice dice,
            Clock clock,
            SurpriseSelector surpriseSelector) {

        this(
                board,
                names,
                colors,
                dice,
                clock,
                surpriseSelector,
                new SurpriseStrategies());
    }

    /** Replace surprise policies without changing the engine's control flow. */
    GameEngine(
            Board board,
            List<String> names,
            List<PlayerColor> colors,
            Dice dice,
            Clock clock,
            SurpriseSelector surpriseSelector,
            SurpriseStrategies surpriseStrategies) {

        Objects.requireNonNull(board, "board");

        SetupValidator.validate(
                board.difficulty(), names, colors)
                .ifPresent(message -> {
                    throw new IllegalArgumentException(message);
                });

        List<Player> players = new ArrayList<>();

        for (int i = 0; i < names.size(); i++) {
            players.add(new Player(
                    i,
                    names.get(i).trim(),
                    colors.get(i)));
        }

        this.state = new GameState(board, players);

        this.dice = Objects.requireNonNull(
                dice, "dice");

        this.surpriseSelector = Objects.requireNonNull(
                surpriseSelector, "surpriseSelector");

        this.surpriseStrategies = Objects.requireNonNull(
                surpriseStrategies, "surpriseStrategies");

        this.regularThrow = DiceThrow.regularFor(
                board.difficulty().multiplierDie(), dice);

        this.surpriseThrow =
                new SurpriseUltraBoostThrow(dice);

        this.gameClock = new GameClock(
                Objects.requireNonNull(clock, "clock"));

        state.log("המשחק התחיל!");
    }

    /** Return the current authoritative game state. */
    public GameState state() {
        return state;
    }

    /** Game time for the clock on the board (FR-48). */
    public long elapsedSeconds() {
        return gameClock.elapsedSeconds();
    }

    /** Register an observer for game events. */
    public void addListener(GameListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    /** Unregister a previously added observer. */
    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    /** FR-15 to FR-21: perform a throw and determine available actions. */
    public void rollDice() {
        if (state.phase() != GameState.Phase.WAITING_FOR_ROLL) {
            rejectAction("בחר כלי לפני הטלה נוספת.");
            return;
        }

        state.setFeedbackMessage(null);

        Player player = state.currentPlayer();

        DiceThrow diceThrow =
                state.turnState().takeSpecialRoll()
                        ? surpriseThrow
                        : regularThrow;

        ThrowResult result = diceThrow.perform(state);

        state.setLastRoll(result.roll());

        state.log(
                player.name() + ": "
                        + diceThrow.describe(result.roll()));

        switch (result.outcome()) {
            case CHOOSE_PIECE -> {
                state.setLegalMoves(result.legalMoves());
                state.setPhase(
                        GameState.Phase.WAITING_FOR_MOVE);
            }

            case ROLL_AGAIN -> {
                state.log(
                        player.name() + ": אין מהלך חוקי");

                state.log(
                        "תור נוסף ל" + player.name() + "!");

                state.setLastRoll(null);
            }

            case PASS_TURN -> {
                state.log(
                        player.name() + ": אין מהלך חוקי");

                passTurn();
            }

            case THIRD_SIX_CANCELLED -> {
                state.log(
                        player.name()
                                + ": שלוש שישיות ברצף - התור בוטל!");

                passTurn();
            }
        }

        notifyChanged();
    }

    /** Report an invalid action without terminating the game. */
    private void rejectAction(String hint) {
        state.setFeedbackMessage(
                state.phase() == GameState.Phase.GAME_OVER
                        ? "המשחק הסתיים."
                        : hint);

        notifyChanged();
    }

    /** Reject illegal selections or commit one approved move. */
    public boolean movePiece(int pieceId) {
        if (state.phase()
                == GameState.Phase.WAITING_FOR_SURPRISE_CHOICE) {

            return selectSurpriseOpponentPiece(pieceId);
        }

        if (state.phase()
                != GameState.Phase.WAITING_FOR_MOVE) {

            rejectAction("הטל קובייה לפני בחירת כלי.");
            return false;
        }

        Optional<Move> move = state.legalMoves()
                .stream()
                .filter(candidate ->
                        candidate.pieceId() == pieceId)
                .findFirst();

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
        if (state.phase()
                != GameState.Phase.WAITING_FOR_SURPRISE_CHOICE) {

            rejectAction("אין כרגע בחירת כלי יריב.");
            return false;
        }

        Optional<Move> move = state.legalMoves()
                .stream()
                .filter(candidate ->
                        candidate.pieceId() == pieceId)
                .findFirst();

        if (move.isEmpty()) {
            rejectAction("בחר כלי יריב מסומן.");
            return false;
        }

        int roll = dice.roll().value();
        int steps = (roll + 1) / 2;

        Optional<Move> actualMove =
                SurpriseMovement.backward(
                        state, pieceId, steps);

        if (actualMove.isEmpty()) {
            rejectAction("לא ניתן להזיז את הכלי שנבחר.");
            return false;
        }

        state.log(
                "קוביית ההפתעה: " + roll
                        + " — תנועה לאחור: " + steps);

        state.setFeedbackMessage(null);
        apply(actualMove.get());

        return true;
    }

    /** Resolve movements and chained surprises before completing the turn. */
    private void apply(Move firstMove) {
        Player activePlayer = state.currentPlayer();

        state.setPhase(
                GameState.Phase.RESOLVING_EFFECTS);

        Move nextMove = firstMove;

        // Iteration avoids growing the call stack for chained surprises.
        while (nextMove != null) {
            Move move = nextMove;

            MovementOutcome outcome =
                    commitMovement(move);

            pendingNotifications.add(
                    () -> publishMovement(move, outcome));

            Piece piece = state.piece(move.pieceId());

            if (RankingManager.isGameOver(state)) {
                endGame();
                flushNotifications();
                return;
            }

            Cell landing = state.board().cellAt(
                    piece.color(), piece.step());

            if (landing == null
                    || landing.type() != CellType.SURPRISE) {

                break;
            }

            SurpriseType type = surpriseSelector.draw();

            SurpriseEvent event = new SurpriseEvent(
                    activePlayer.index(),
                    piece.id(),
                    type);

            state.log(
                    "הפתעה: " + type.displayName());

            pendingNotifications.add(() -> {
                for (GameListener listener
                        : List.copyOf(listeners)) {

                    listener.onSurpriseDrawn(event);
                }
            });

            // Strategy: select the effect through the common interface.
            SurpriseStrategy strategy =
                    surpriseStrategies.forType(type);

            SurpriseResolution resolution =
                    Objects.requireNonNull(
                            strategy.apply(state, piece.id()),
                            "strategy result");

            nextMove = resolution.movement()
                    .orElse(null);

            if (resolution.requiresOpponentSelection()) {
                offerOpponentMoves(activePlayer);
            }

            if (state.phase()
                    == GameState.Phase.WAITING_FOR_SURPRISE_CHOICE) {

                flushNotifications();
                return;
            }
        }

        endTurnAfterMove(
                activePlayer,
                activePlayer.isFinished());

        flushNotifications();
    }

    /** Offer movable opponent pawns; roll only after a valid selection. */
    private void offerOpponentMoves(Player activePlayer) {
        List<Move> options = new ArrayList<>();

        for (Piece candidate : state.pieces()) {
            if (candidate.ownerIndex()
                    == activePlayer.index()) {

                continue;
            }

            if (state.players()
                    .get(candidate.ownerIndex())
                    .isFinished()) {

                continue;
            }

            SurpriseMovement.backward(
                    state, candidate.id(), 1)
                    .ifPresent(options::add);
        }

        if (options.isEmpty()) {
            state.log(
                    "ההפתעה בוטלה: אין כלי יריב שניתן להזיז.");

            return;
        }

        state.setLegalMoves(options);

        state.setPhase(
                GameState.Phase.WAITING_FOR_SURPRISE_CHOICE);

        state.setFeedbackMessage(
                "בחר כלי יריב מסומן להזזה לאחור.");
    }

    /** Publish movement events before the following surprise notification. */
    private void publishMovement(
            Move move, MovementOutcome outcome) {

        for (GameListener listener
                : List.copyOf(listeners)) {

            listener.onPieceMoved(move);
        }

        if (outcome.interaction().hasVisualEffect()) {
            for (GameListener listener
                    : List.copyOf(listeners)) {

                listener.onPieceInteraction(
                        outcome.interaction());
            }
        }

        if (outcome.playerFinish() != null) {
            for (GameListener listener
                    : List.copyOf(listeners)) {

                listener.onPlayerFinished(
                        outcome.playerFinish());
            }
        }
    }

    /** Refresh observers after reaching a stable state or awaiting a choice. */
    private void flushNotifications() {
        List<Runnable> notifications =
                List.copyOf(pendingNotifications);

        pendingNotifications.clear();

        for (Runnable notification : notifications) {
            notification.run();
        }

        notifyChanged();

        if (state.phase()
                == GameState.Phase.GAME_OVER) {

            for (GameListener listener
                    : List.copyOf(listeners)) {

                listener.onGameOver(state.result());
            }
        }
    }

    /**
     * Commit an already validated movement.
     * Turn completion and event publication are handled separately.
     */
    private MovementOutcome commitMovement(Move move) {
        Piece piece = state.piece(move.pieceId());

        Player owner = state.players()
                .get(piece.ownerIndex());

        Position from = state.positionOf(piece);

        Position to = state.board().positionOf(
                piece.color(), move.toStep());

        Map<Position, Set<Integer>> before =
                snapshotBlocks(from, to);

        piece.moveTo(move.toStep());
        state.setLegalMoves(List.of());

        PieceInteractionEvent.Capture capture = null;

        Optional<Piece> captured =
                InteractionRules.capturableOpponent(
                        state, piece);

        if (captured.isPresent()) {
            Piece victim = captured.get();

            capture = new PieceInteractionEvent.Capture(
                    victim.id(), to);

            victim.returnToCastle();

            state.log(
                    owner.name() + " אכל כלי של "
                            + state.players()
                                    .get(victim.ownerIndex())
                                    .name()
                            + "!");
        }

        Position protection =
                InteractionRules
                        .sharesProtectedSquareWithOpponent(
                                state, piece)
                        ? to
                        : null;

        Position blockedBy = null;

        if (move.partial()) {
            int direction = Integer.compare(
                    move.toStep(), move.fromStep());

            blockedBy = state.board().positionOf(
                    piece.color(),
                    move.toStep() + direction);
        }

        PieceInteractionEvent interaction =
                new PieceInteractionEvent(
                        capture,
                        protection,
                        blockedBy,
                        changesToBlocks(before));

        if (state.isFinished(piece)) {
            state.log(
                    owner.name() + " הכניס כלי לבית הסיום!");
        }

        PlayerFinishEvent playerFinish =
                rankIfAllPiecesHome(owner);

        return new MovementOutcome(
                interaction, playerFinish);
    }

    /** Events retained until the engine is ready to publish them. */
    private record MovementOutcome(
            PieceInteractionEvent interaction,
            PlayerFinishEvent playerFinish) {
    }

    /** FR-45: rank a player after all four pieces reach the finish. */
    private PlayerFinishEvent rankIfAllPiecesHome(
            Player player) {

        if (!RankingManager.rankIfAllPiecesHome(
                state, player)) {

            return null;
        }

        state.log(
                "מקום " + player.rank()
                        + ": " + player.name());

        return new PlayerFinishEvent(
                player.index(),
                player.name(),
                player.color(),
                player.rank());
    }

    /** Complete the turn only after all landing effects have been resolved. */
    private boolean endTurnAfterMove(
            Player player, boolean justFinished) {

        if (justFinished
                && RankingManager.isGameOver(state)) {

            endGame();
            return true;
        }

        switch (TurnManager.afterMove(
                state, player, state.lastRoll())) {

            case SPECIAL_ROLL -> {
                state.setLastRoll(null);

                state.setPhase(
                        GameState.Phase.WAITING_FOR_ROLL);

                state.log(
                        "הטלה מיוחדת ל" + player.name()
                                + ": קובייה רגילה + UltraBoost!");
            }

            case ROLL_AGAIN -> {
                state.setLastRoll(null);

                state.setPhase(
                        GameState.Phase.WAITING_FOR_ROLL);

                state.log(
                        "תור נוסף ל" + player.name() + "!");
            }

            case PASS_TURN -> passTurn();
        }

        return false;
    }

    /** Snapshot Double Block owners at the affected squares. */
    private Map<Position, Set<Integer>> snapshotBlocks(
            Position from, Position to) {

        Map<Position, Set<Integer>> snapshot =
                new LinkedHashMap<>();

        if (from != null) {
            snapshot.put(
                    from,
                    InteractionRules.blockOwners(
                            state, from));
        }

        if (to != null) {
            snapshot.put(
                    to,
                    InteractionRules.blockOwners(
                            state, to));
        }

        return snapshot;
    }

    /** Compare block owners before and after a movement. */
    private List<PieceInteractionEvent.BlockChange>
            changesToBlocks(
                    Map<Position, Set<Integer>> before) {

        List<PieceInteractionEvent.BlockChange> changes =
                new ArrayList<>();

        for (Map.Entry<Position, Set<Integer>> entry
                : before.entrySet()) {

            Position square = entry.getKey();

            Set<Integer> oldOwners =
                    entry.getValue();

            Set<Integer> newOwners =
                    InteractionRules.blockOwners(
                            state, square);

            for (Integer owner : oldOwners) {
                if (!newOwners.contains(owner)) {
                    changes.add(
                            new PieceInteractionEvent.BlockChange(
                                    owner,
                                    square,
                                    false));
                }
            }

            for (Integer owner : newOwners) {
                if (!oldOwners.contains(owner)) {
                    changes.add(
                            new PieceInteractionEvent.BlockChange(
                                    owner,
                                    square,
                                    true));
                }
            }
        }

        return List.copyOf(changes);
    }

    /** FR-47 and FR-48: stop the clock, rank remaining players and lock the game. */
    private void endGame() {
        gameClock.stop();

        RankingManager.rankRemainingPlayers(state);

        GameResult result = new GameResult(
                state.difficulty(),
                RankingManager.standings(state),
                gameClock.elapsedSeconds(),
                gameClock.endedAt());

        state.setResult(result);
        state.setLegalMoves(List.of());
        state.setLastRoll(null);
        state.setPhase(GameState.Phase.GAME_OVER);
        state.log("המשחק הסתיים!");
    }

    /** Advance to the next eligible player. */
    private void passTurn() {
        TurnManager.passTurn(state);
    }

    /** Notify observers of the current authoritative state. */
    private void notifyChanged() {
        for (GameListener listener
                : List.copyOf(listeners)) {

            listener.onStateChanged(state);
        }
    }
}