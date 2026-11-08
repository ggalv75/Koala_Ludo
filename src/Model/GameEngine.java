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
    private final Dice dice;
    private final GameClock gameClock;
    private final List<GameListener> listeners = new ArrayList<>();

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
        state.log("המשחק התחיל. התור של " + state.currentPlayer().name());
    }

    /** Return the current authoritative game state. */
    public GameState state() { return state; }

    /** Game time for the clock on the board (FR-48). */
    public long elapsedSeconds() { return gameClock.elapsedSeconds(); }

    /** Register one observer for game state changes. */
    public void addListener(GameListener listener) { listeners.add(Objects.requireNonNull(listener)); }

    /** Unregister a previously added observer. */
    public void removeListener(GameListener listener) { listeners.remove(listener); }

    /** FR-15, FR-18, FR-19, FR-20: rolls the die and works out what the player may do. */
    public void rollDice() {
        if (state.phase() != GameState.Phase.WAITING_FOR_ROLL) {
            rejectAction("יש לבחור כלי להזזה לפני הטלה נוספת.");
            return;
        }
        DiceRoll roll = dice.roll();
        state.setLastRoll(roll);
        state.log(state.currentPlayer().name() + " הטיל/ה " + roll.value());

        List<Move> moves = MoveValidator.legalMoves(state, roll);
        switch (TurnManager.afterRoll(state, roll, !moves.isEmpty())) {
            case CHOOSE_PIECE -> {
                state.setLegalMoves(moves);
                state.setPhase(GameState.Phase.WAITING_FOR_MOVE);
                state.log("בחרו כלי מסומן להזזה.");
            }
            case ROLL_AGAIN -> {
                state.log("אין מהלך חוקי, אבל 6 מזכה בהטלה נוספת.");
                state.setLastRoll(null);
            }
            case PASS_TURN -> {
                state.log("אין מהלך חוקי. התור עובר.");
                passTurn();
            }
            case THIRD_SIX_CANCELLED -> {
                state.log("שלוש הטלות 6 ברצף: ההטלה השלישית מבוטלת והתור עובר.");
                passTurn();
            }
        }
        notifyChanged();
    }

    /** NFR-01: an action that is not allowed now only shows a message to the player. */
    private void rejectAction(String hint) {
        state.log(state.phase() == GameState.Phase.GAME_OVER ? "המשחק הסתיים." : hint);
        notifyChanged();
    }

    /** Reject illegal selections or commit exactly one approved move. */
    public boolean movePiece(int pieceId) {
        if (state.phase() != GameState.Phase.WAITING_FOR_MOVE) {
            state.log(state.phase() == GameState.Phase.GAME_OVER
                    ? "המשחק הסתיים."
                    : "יש להטיל קובייה לפני בחירת כלי.");
            notifyChanged();
            return false;
        }
        Optional<Move> move = state.legalMoves().stream().filter(m -> m.pieceId() == pieceId).findFirst();
        if (move.isEmpty()) {
            state.log("לכלי הזה אין מהלך חוקי בהטלה הנוכחית. בחרו כלי מסומן.");
            notifyChanged();
            return false;
        }
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
        state.log(describe(player, piece, move));

        PieceInteractionEvent.Capture capture = null;
        Optional<Piece> captured = InteractionRules.capturableOpponent(state, piece);
        if (captured.isPresent()) {
            Piece victim = captured.get();
            capture = new PieceInteractionEvent.Capture(victim.id(), to);
            victim.returnToCastle();
            state.log("אכילה! כלי " + victim.number() + " של "
                    + state.players().get(victim.ownerIndex()).name() + " חזר לטירה.");
        }
        Position protection = InteractionRules.sharesProtectedSquareWithOpponent(state, piece) ? to : null;
        Position blockedBy = move.partial()
                ? state.board().positionOf(piece.color(), move.toStep() + 1) : null;
        PieceInteractionEvent interaction = new PieceInteractionEvent(
                capture, protection, blockedBy, changesToBlocks(before));

        if (state.isFinished(piece)) {
            state.log("כלי " + piece.number() + " של " + player.name() + " הגיע לבית הסיום.");
        }
        PlayerFinishEvent playerFinish = rankIfAllPiecesHome(player);
        boolean gameOver = endTurnAfterMove(player, playerFinish != null);
        notifyMoved(move, interaction, playerFinish);
        if (gameOver) {
            for (GameListener listener : List.copyOf(listeners)) listener.onGameOver(state.result());
        }
    }

    /** FR-45: ranks the player when all four pieces are home. Returns the finish event, or null. */
    private PlayerFinishEvent rankIfAllPiecesHome(Player player) {
        if (!RankingManager.rankIfAllPiecesHome(state, player)) return null;
        state.log(player.name() + " סיים/ה במקום " + player.rank() + ".");
        return new PlayerFinishEvent(player.index(), player.name(), player.color(), player.rank());
    }

    /** End the game, grant an extra roll or pass the turn; return whether play ended. */
    private boolean endTurnAfterMove(Player player, boolean justFinished) {
        if (justFinished && RankingManager.isGameOver(state)) {
            endGame();
            return true;
        }
        if (TurnManager.rollsAgain(player, state.lastRoll())) {
            state.setLastRoll(null);
            state.setPhase(GameState.Phase.WAITING_FOR_ROLL);
            state.log("בזכות ה-6, " + player.name() + " מטיל/ה שוב.");
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
        state.log("המשחק הסתיים. המנצח/ת: " + result.winner().name());
    }

    /** Advance to the next unfinished player. */
    private void passTurn() {
        TurnManager.passTurn(state);
        state.log("התור של " + state.currentPlayer().name());
    }

    /** Build a short human-readable message for a committed move. */
    private String describe(Player player, Piece piece, Move move) {
        if (move.release()) return player.name() + " הוציא/ה את כלי " + piece.number() + " מהטירה.";
        String text = player.name() + " הזיז/ה את כלי " + piece.number() + " " + move.steps() + " צעדים";
        return move.partial() ? text + " ונעצר/ה לפני חסימה." : text + ".";
    }

    /** Publish immutable movement and interaction events before refreshing observers. */
    private void notifyMoved(Move move, PieceInteractionEvent interaction, PlayerFinishEvent playerFinish) {
        for (GameListener listener : List.copyOf(listeners)) listener.onPieceMoved(move);
        if (interaction.hasVisualEffect()) {
            for (GameListener listener : List.copyOf(listeners)) listener.onPieceInteraction(interaction);
        }
        if (playerFinish != null) {
            for (GameListener listener : List.copyOf(listeners)) listener.onPlayerFinished(playerFinish);
        }
        notifyChanged();
    }

    /** Notify every registered observer of the current authoritative state. */
    private void notifyChanged() {
        for (GameListener listener : List.copyOf(listeners)) listener.onStateChanged(state);
    }
}
