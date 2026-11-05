package Model;

import java.time.Clock;
import java.time.LocalDateTime;
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
    private final Clock clock;
    private final GameClock gameClock;
    private final List<GameListener> listeners = new ArrayList<>();

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
        this.clock = Objects.requireNonNull(clock);
        this.gameClock = new GameClock(clock);
        state.log("המשחק התחיל. התור של " + state.currentPlayer().name());
    }

    public GameState state() { return state; }

    /** Game time for the clock on the board (FR-48). */
    public long elapsedSeconds() { return gameClock.elapsedSeconds(); }

    public void addListener(GameListener listener) { listeners.add(Objects.requireNonNull(listener)); }

    public void removeListener(GameListener listener) { listeners.remove(listener); }

    /** FR-15, FR-18, FR-19, FR-20: rolls the die and works out what the player may do. */
    public void rollDice() {
        if (state.phase() != GameState.Phase.WAITING_FOR_ROLL) {
            state.log(state.phase() == GameState.Phase.GAME_OVER
                    ? "המשחק הסתיים."
                    : "יש לבחור כלי להזזה לפני הטלה נוספת.");
            notifyChanged();
            return;
        }
        Player player = state.currentPlayer();
        DiceRoll roll = dice.roll();
        state.setLastRoll(roll);
        state.log(player.name() + " הטיל/ה " + roll.value());

        if (TurnManager.registerRoll(state, roll)) {
            state.log("שלוש הטלות 6 ברצף: ההטלה השלישית מבוטלת והתור עובר.");
            passTurn();
            notifyChanged();
            return;
        }

        List<Move> moves = MoveValidator.legalMoves(state, roll);
        if (!moves.isEmpty()) {
            state.setLegalMoves(moves);
            state.setPhase(GameState.Phase.WAITING_FOR_MOVE);
            state.log("בחרו כלי מסומן להזזה.");
        } else if (roll.isSix()) {
            state.log("אין מהלך חוקי, אבל 6 מזכה בהטלה נוספת.");
        } else {
            state.log("אין מהלך חוקי. התור עובר.");
            passTurn();
        }
        notifyChanged();
    }

    /**
     * FR-23, FR-27: moves the chosen piece if it has a legal move.
     * Returns false (with a message) for a piece that cannot move now.
     */
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

    private void apply(Move move) {
        Player player = state.currentPlayer();
        Piece piece = state.piece(move.pieceId());
        piece.moveTo(move.toStep());
        state.setLegalMoves(List.of());
        state.log(describe(player, piece, move));

        InteractionRules.capturableOpponent(state, piece).ifPresent(victim -> {
            victim.returnToCastle();
            state.log("אכילה! כלי " + victim.number() + " של "
                    + state.players().get(victim.ownerIndex()).name() + " חזר לטירה.");
        });
        if (state.isFinished(piece)) {
            state.log("כלי " + piece.number() + " של " + player.name() + " הגיע לבית הסיום.");
        }
        if (RankingManager.rankIfCompleted(state, player)) {
            state.log(player.name() + " סיים/ה במקום " + player.rank() + ".");
            if (RankingManager.isGameOver(state)) {
                endGame(move);
                return;
            }
        }
        if (TurnManager.grantsExtraRoll(state)) {
            state.setPhase(GameState.Phase.WAITING_FOR_ROLL);
            state.log("בזכות ה-6, " + player.name() + " מטיל/ה שוב.");
        } else {
            passTurn();
        }
        notifyMoved(move);
    }

    private void endGame(Move move) {
        gameClock.stop();
        List<GameResult.Standing> standings = new ArrayList<>();
        for (Player player : RankingManager.finalStandings(state)) {
            standings.add(new GameResult.Standing(player.rank(), player.name(), player.color(),
                    player.completedAllPieces()));
        }
        GameResult result = new GameResult(state.difficulty(), standings,
                gameClock.elapsedSeconds(), LocalDateTime.now(clock));
        state.setResult(result);
        state.setLegalMoves(List.of());
        state.setPhase(GameState.Phase.GAME_OVER);
        state.log("המשחק הסתיים. המנצח/ת: " + result.winner().name());
        notifyMoved(move);
        for (GameListener listener : List.copyOf(listeners)) listener.onGameOver(result);
    }

    private void passTurn() {
        TurnManager.passTurn(state);
        state.log("התור של " + state.currentPlayer().name());
    }

    private String describe(Player player, Piece piece, Move move) {
        if (move.release()) return player.name() + " הוציא/ה את כלי " + piece.number() + " מהטירה.";
        String text = player.name() + " הזיז/ה את כלי " + piece.number() + " " + move.steps() + " צעדים";
        return move.partial() ? text + " ונעצר/ה לפני חסימה." : text + ".";
    }

    /** Notify presentation observers of the committed move before the new state. */
    private void notifyMoved(Move move) {
        for (GameListener listener : List.copyOf(listeners)) listener.onPieceMoved(move);
        notifyChanged();
    }

    private void notifyChanged() {
        for (GameListener listener : List.copyOf(listeners)) listener.onStateChanged(state);
    }
}
