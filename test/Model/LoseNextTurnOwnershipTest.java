package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Regression: a chained surprise belongs to the landing piece's owner. */
class LoseNextTurnOwnershipTest {
    @Test void penaltyTargetsOpponentOwnerWithoutForfeitingActivePlayersSix() {
        GameState state = new GameEngine(Difficulty.EASY,
                List.of("Dor", "Gal"), List.of(PlayerColor.RED, PlayerColor.GREEN)).state();
        state.piece(4).moveTo(10);
        new SurpriseStrategies().forType(SurpriseType.LOSE_NEXT_TURN).apply(state, 4);
        assertFalse(state.losesNextTurn(state.players().get(0)));
        assertTrue(state.losesNextTurn(state.players().get(1)));
        assertEquals(0, state.currentPlayerIndex());
        assertEquals(TurnManager.AfterMove.ROLL_AGAIN,
                TurnManager.afterMove(state, state.currentPlayer(), new DiceRoll(6)));
    }

    @Test void ownPieceStillUsesExistingCurrentPlayerPenaltyRules() {
        GameState state = new GameEngine(Difficulty.EASY,
                List.of("Dor", "Gal"), List.of(PlayerColor.RED, PlayerColor.GREEN)).state();
        state.piece(0).moveTo(10);
        new SurpriseStrategies().forType(SurpriseType.LOSE_NEXT_TURN).apply(state, 0);
        assertTrue(state.losesNextTurn(state.players().get(0)));
        assertFalse(state.losesNextTurn(state.players().get(1)));
        assertEquals(TurnManager.AfterMove.PASS_TURN,
                TurnManager.afterMove(state, state.currentPlayer(), new DiceRoll(6)));
    }

    @Test void chainedOpponentPenaltySkipsOpponentAndPassesToThirdPlayer() {
        GameEngine engine = chainedGame(3);
        assertTrue(engine.movePiece(0));
        assertEquals(GameState.Phase.WAITING_FOR_SURPRISE_CHOICE, engine.state().phase());
        assertTrue(engine.selectSurpriseOpponentPiece(4));
        assertEquals(2, engine.state().currentPlayerIndex());
        assertFalse(engine.state().losesNextTurn(engine.state().players().get(0)));
        assertFalse(engine.state().losesNextTurn(engine.state().players().get(1)),
                "The opponent's penalty is consumed by skipping that opponent once");
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, engine.state().phase());
    }

    @Test void chainedOpponentPenaltyReturnsToActivePlayerInTwoPlayerGame() {
        GameEngine engine = chainedGame(2);
        assertTrue(engine.movePiece(0));
        assertTrue(engine.selectSurpriseOpponentPiece(4));
        assertEquals(0, engine.state().currentPlayerIndex());
        assertFalse(engine.state().losesNextTurn(engine.state().players().get(0)));
        assertFalse(engine.state().losesNextTurn(engine.state().players().get(1)));
    }

    private static GameEngine chainedGame(int count) {
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
        Random draws = new Random() {
            private static final long serialVersionUID = 1L;
            private int index;
            @Override public int nextInt(int bound) {
                assertEquals(100, bound);
                assertTrue(index < 2, "Only two surprises should be drawn");
                return index++ == 0 ? 10 : 60;
            }
        };
        Dice dice = new Dice() {
            @Override public DiceRoll roll() { return new DiceRoll(1); }
        };
        GameEngine engine = new GameEngine(
                new Board(base.difficulty(), cells, routes, castles, base.centerCorner()),
                List.of("Dor", "Gal", "Omer").subList(0, count),
                List.of(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE).subList(0, count),
                dice, Clock.systemUTC(), new SurpriseSelector(draws));
        GameState state = engine.state();
        state.piece(0).moveTo(5);
        int opponentStep = base.route(PlayerColor.GREEN)
                .indexOf(base.positionOf(PlayerColor.RED, 10));
        assertTrue(opponentStep > 0);
        state.piece(4).moveTo(opponentStep);
        state.setLastRoll(new DiceRoll(1));
        state.setLegalMoves(List.of(new Move(0, 5, 6, false, false)));
        state.setPhase(GameState.Phase.WAITING_FOR_MOVE);
        return engine;
    }
}
