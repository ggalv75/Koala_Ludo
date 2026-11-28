package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * FR-44, FR-25: the UltraBoost surprise throw belongs to the owner of the piece that drew it.
 * An opponent's piece that reaches a surprise in a chain gets the throw at the start of its
 * owner's next turn, never the active player.
 */
class SpecialRollOwnershipTest {

    @Test
    void opponentPieceDefersTheThrowToItsOwnersNextTurn() {
        GameState state = newState(2);
        state.piece(4).moveTo(10);

        new SurpriseStrategies().forType(SurpriseType.ULTRA_BOOST_ROLL).apply(state, 4);

        assertFalse(state.specialRollPending(), "The active player gets no special throw");
        assertTrue(state.turnState().hasDeferredSpecialRoll(1));

        TurnManager.passTurn(state);

        assertEquals(1, state.currentPlayerIndex());
        assertTrue(state.specialRollPending());
        assertEquals(MultiplierDie.ULTRA_BOOST, state.nextThrowMultiplier());
        assertFalse(state.turnState().hasDeferredSpecialRoll(1), "The deferred throw is used once");
    }

    @Test
    void ownPieceKeepsTheImmediateSpecialThrow() {
        GameState state = newState(2);
        state.piece(0).moveTo(10);

        new SurpriseStrategies().forType(SurpriseType.ULTRA_BOOST_ROLL).apply(state, 0);

        assertTrue(state.specialRollPending());
        assertFalse(state.turnState().hasDeferredSpecialRoll(0));
        assertEquals(TurnManager.AfterMove.SPECIAL_ROLL,
                TurnManager.afterMove(state, state.currentPlayer(), new DiceRoll(1)));
    }

    @Test
    void deferredThrowSkipsPlayersBetween() {
        GameState state = newState(3);
        TurnManager.grantSpecialRoll(state, state.players().get(2));

        TurnManager.passTurn(state);
        assertEquals(1, state.currentPlayerIndex());
        assertFalse(state.specialRollPending(), "Player 1 did not draw the surprise");

        TurnManager.passTurn(state);
        assertEquals(2, state.currentPlayerIndex());
        assertTrue(state.specialRollPending());
    }

    @Test
    void lostTurnGrantedAfterTheThrowCancelsIt() {
        GameState state = newState(3);
        TurnManager.grantSpecialRoll(state, state.players().get(1));
        TurnManager.loseNextTurn(state, state.players().get(1));

        assertNoSpecialThrowForPlayerOne(state);
    }

    @Test
    void lostTurnGrantedBeforeTheThrowCancelsIt() {
        GameState state = newState(3);
        TurnManager.loseNextTurn(state, state.players().get(1));
        TurnManager.grantSpecialRoll(state, state.players().get(1));

        assertNoSpecialThrowForPlayerOne(state);
    }

    @Test
    void chainedOpponentSurpriseGivesTheThrowToTheOpponent() {
        GameEngine engine = chainedGame();
        assertTrue(engine.movePiece(0));
        assertEquals(GameState.Phase.WAITING_FOR_SURPRISE_CHOICE, engine.state().phase());

        assertTrue(engine.selectSurpriseOpponentPiece(4));

        GameState state = engine.state();
        assertEquals(1, state.currentPlayerIndex(), "The active player does not keep the turn");
        assertTrue(state.specialRollPending(), "The opponent starts with the UltraBoost throw");
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());

        engine.rollDice();
        assertFalse(state.specialRollPending());
        assertEquals(0, state.consecutiveSixes(), "FR-21: the special throw does not count toward sixes");
    }

    private static void assertNoSpecialThrowForPlayerOne(GameState state) {
        TurnManager.passTurn(state);
        assertEquals(2, state.currentPlayerIndex(), "Player 1 is skipped once");
        assertFalse(state.specialRollPending());
        TurnManager.passTurn(state);
        TurnManager.passTurn(state);
        assertEquals(1, state.currentPlayerIndex());
        assertFalse(state.specialRollPending(), "The cancelled throw does not move to a later turn");
    }

    private static GameState newState(int count) {
        return new GameEngine(Difficulty.EASY,
                List.of("Dor", "Gal", "Omer").subList(0, count),
                List.of(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE).subList(0, count)).state();
    }

    /** Red lands on a surprise (opponent backward), Green is pushed onto a second surprise (UltraBoost). */
    private static GameEngine chainedGame() {
        Board base = BoardFactory.create(Difficulty.EASY, new Random(1));
        Map<Position, Cell> cells = new LinkedHashMap<>(base.cells());
        cells.put(base.positionOf(PlayerColor.RED, 6), new Cell(CellType.SURPRISE, null));
        cells.put(base.positionOf(PlayerColor.RED, 9), new Cell(CellType.SURPRISE, null));
        cells.put(base.positionOf(PlayerColor.RED, 10), Cell.REGULAR);
        Map<PlayerColor, List<Position>> routes = new EnumMap<>(PlayerColor.class);
        Map<PlayerColor, Position> castles = new EnumMap<>(PlayerColor.class);
        for (PlayerColor color : PlayerColor.values()) {
            routes.put(color, base.route(color));
            castles.put(color, base.castleCorner(color));
        }
        int[] tickets = {10, 95};
        Random draws = new Random() {
            private static final long serialVersionUID = 1L;
            private int index;
            @Override public int nextInt(int bound) {
                assertEquals(100, bound);
                assertTrue(index < tickets.length, "Only two surprises should be drawn");
                return tickets[index++];
            }
        };
        Dice dice = new Dice() {
            @Override public DiceRoll roll() { return new DiceRoll(1); }
        };
        GameEngine engine = new GameEngine(
                new Board(base.difficulty(), cells, routes, castles, base.centerCorner()),
                List.of("Dor", "Gal"), List.of(PlayerColor.RED, PlayerColor.GREEN),
                dice, Clock.systemUTC(), new SurpriseSelector(draws));
        GameState state = engine.state();
        state.piece(0).moveTo(5);
        int opponentStep = base.route(PlayerColor.GREEN).indexOf(base.positionOf(PlayerColor.RED, 10));
        assertTrue(opponentStep > 0);
        state.piece(4).moveTo(opponentStep);
        state.setLastRoll(new DiceRoll(1));
        state.setLegalMoves(List.of(new Move(0, 5, 6, false, false)));
        state.setPhase(GameState.Phase.WAITING_FOR_MOVE);
        return engine;
    }
}
