package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Integration tests use a controlled board; Easy gameplay is not given surprise cells. */
class SurpriseEngineTest {

    @Test
    void directFinishMovesTheLandingPieceHome() {
        GameEngine engine = prepared(0, 5, 6);
        assertTrue(engine.movePiece(0));
        assertEquals(engine.state().board().finishStep(), engine.state().piece(0).step());
        assertEquals(1, engine.state().currentPlayerIndex());
    }

    @Test
    void directFinishCanEndAndRankTheGame() {
        GameEngine engine = prepared(0, 5, 6);
        GameState state = engine.state();
        for (int id = 1; id <= 3; id++) state.piece(id).moveTo(state.board().finishStep());
        List<String> events = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onPlayerFinished(PlayerFinishEvent event) { events.add("rank"); }
            @Override public void onGameOver(GameResult result) { events.add("end"); }
        });
        engine.movePiece(0);
        assertEquals(GameState.Phase.GAME_OVER, state.phase());
        assertEquals(1, state.players().get(0).rank());
        assertEquals(2, state.players().get(1).rank());
        assertEquals(List.of("rank", "end"), events);
    }

    @Test
    void castleSurpriseReturnsTheLandingPieceToItsCastle() {
        GameEngine engine = prepared(20, 5, 6);
        engine.movePiece(0);
        assertTrue(engine.state().piece(0).isInCastle());
    }

    @Test
    void backwardSurpriseCapturesAnOpponentOnAnUnprotectedSquare() {
        GameEngine engine = prepared(30, 5, 6);
        putOpponentAtOwnStep(engine.state(), 4, 3);
        engine.movePiece(0);
        assertEquals(3, engine.state().piece(0).step());
        assertTrue(engine.state().piece(4).isInCastle());
    }

    @Test
    void backwardSurpriseStopsBeforeAnOpponentBlock() {
        GameEngine engine = prepared(30, 5, 6);
        putOpponentAtOwnStep(engine.state(), 4, 4);
        putOpponentAtOwnStep(engine.state(), 5, 4);
        List<PieceInteractionEvent> effects = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onPieceInteraction(PieceInteractionEvent event) { effects.add(event); }
        });
        engine.movePiece(0);
        assertEquals(5, engine.state().piece(0).step());
        Position block = engine.state().board().positionOf(PlayerColor.RED, 4);
        assertTrue(effects.stream().anyMatch(event -> block.equals(event.blockedBy())));
    }

    @Test
    void lostTurnCancelsTheExtraRollEarnedBySix() {
        GameEngine engine = prepared(60, 5, 6);
        engine.state().setLastRoll(new DiceRoll(6));
        engine.movePiece(0);
        assertEquals(1, engine.state().currentPlayerIndex());
        assertTrue(engine.state().losesNextTurn(engine.state().players().get(0)));
    }

    @Test
    void ultraBoostSurpriseKeepsThePlayerAndSchedulesTheSpecialRoll() {
        GameEngine engine = prepared(90, 5, 6);
        engine.movePiece(0);
        assertEquals(0, engine.state().currentPlayerIndex());
        assertTrue(engine.state().specialRollPending());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, engine.state().phase());
        assertEquals(MultiplierDie.ULTRA_BOOST, engine.state().nextThrowMultiplier());
    }

    @Test
    void opponentChoiceWaitsThenRoundsFiveUpToThreeBackwardSteps() {
        GameEngine engine = prepared(10, 5, 6);
        engine.state().piece(4).moveTo(10);
        engine.movePiece(0);
        assertEquals(GameState.Phase.WAITING_FOR_SURPRISE_CHOICE, engine.state().phase());
        assertEquals(0, engine.state().currentPlayerIndex());
        assertEquals(10, engine.state().piece(4).step());
        assertTrue(engine.selectSurpriseOpponentPiece(4));
        assertEquals(7, engine.state().piece(4).step());
        assertEquals(1, engine.state().currentPlayerIndex());
    }

    @Test
    void invalidOpponentChoicePreservesThePendingChoice() {
        GameEngine engine = prepared(10, 5, 6);
        engine.state().piece(4).moveTo(10);
        engine.movePiece(0);
        assertFalse(engine.selectSurpriseOpponentPiece(0));
        assertFalse(engine.selectSurpriseOpponentPiece(999));
        assertEquals(GameState.Phase.WAITING_FOR_SURPRISE_CHOICE, engine.state().phase());
        assertEquals(10, engine.state().piece(4).step());
    }

    @Test
    void rollingIsRejectedWhileAnOpponentChoiceIsPending() {
        GameEngine engine = prepared(10, 5, 6);
        engine.state().piece(4).moveTo(10);
        engine.movePiece(0);
        engine.rollDice();
        assertEquals(GameState.Phase.WAITING_FOR_SURPRISE_CHOICE, engine.state().phase());
        assertEquals(0, engine.state().currentPlayerIndex());
    }

    @Test
    void opponentSurpriseCancelsWhenNoOpponentCanMove() {
        GameEngine engine = prepared(10, 5, 6);
        engine.movePiece(0);
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, engine.state().phase());
        assertEquals(1, engine.state().currentPlayerIndex());
        assertTrue(engine.state().legalMoves().isEmpty());
    }

    @Test
    void surpriseSquareProtectsAnOpponentAlreadyOnIt() {
        GameEngine engine = prepared(20, 5, 6);
        putOpponentAtOwnStep(engine.state(), 4, 6);
        int opponentStep = engine.state().piece(4).step();
        engine.movePiece(0);
        assertEquals(opponentStep, engine.state().piece(4).step());
    }

    @Test
    void chainedSurprisesPublishMovementsAndDrawsInOrder() {
        GameEngine engine = prepared(new ScriptedRandom(30, 0), 5, 6, 3);
        List<String> events = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onPieceMoved(Move move) { events.add("move:" + move.toStep()); }
            @Override public void onSurpriseDrawn(SurpriseEvent event) { events.add("draw:" + event.type()); }
        });
        engine.movePiece(0);
        assertEquals(List.of(
                "move:6", "draw:MOVE_BACKWARD_THREE",
                "move:3", "draw:DIRECT_FINISH",
                "move:" + engine.state().board().finishStep()), events);
        assertEquals(1, engine.state().currentPlayerIndex());
    }

    @Test
    void ordinaryLandingDoesNotDrawASurprise() {
        GameEngine engine = prepared(0, 5, 9);
        List<SurpriseEvent> draws = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onSurpriseDrawn(SurpriseEvent event) { draws.add(event); }
        });
        engine.movePiece(0);
        assertEquals(6, engine.state().piece(0).step());
        assertTrue(draws.isEmpty());
    }

    private static GameEngine prepared(int ticket, int die, int... surpriseSteps) {
        return prepared(new ScriptedRandom(ticket), die, surpriseSteps);
    }

    private static GameEngine prepared(Random surpriseRandom, int die, int... surpriseSteps) {
        Board base = BoardFactory.create(Difficulty.EASY, new Random(1));
        Map<Position, Cell> cells = new LinkedHashMap<>(base.cells());
        // Explicitly control capture test squares, independently of random safe placement.
        cells.put(base.positionOf(PlayerColor.RED, 3), new Cell(CellType.REGULAR, null));
        for (int step : surpriseSteps) {
            cells.put(base.positionOf(PlayerColor.RED, step), new Cell(CellType.SURPRISE, null));
        }
        Map<PlayerColor, List<Position>> routes = new EnumMap<>(PlayerColor.class);
        Map<PlayerColor, Position> castles = new EnumMap<>(PlayerColor.class);
        for (PlayerColor color : PlayerColor.values()) {
            routes.put(color, base.route(color));
            castles.put(color, base.castleCorner(color));
        }
        Board board = new Board(base.difficulty(), cells, routes, castles, base.centerCorner());
        Dice dice = new Dice(new Random(2)) {
            @Override public DiceRoll roll() { return new DiceRoll(die); }
        };
        GameEngine engine = new GameEngine(board,
                List.of("דור", "גל"), List.of(PlayerColor.RED, PlayerColor.GREEN),
                dice, Clock.systemUTC(), new SurpriseSelector(surpriseRandom));
        GameState state = engine.state();
        state.piece(0).moveTo(5);
        state.setLastRoll(new DiceRoll(1));
        state.setLegalMoves(List.of(new Move(0, 5, 6, false, false)));
        state.setPhase(GameState.Phase.WAITING_FOR_MOVE);
        return engine;
    }

    private static void putOpponentAtOwnStep(GameState state, int pieceId, int ownStep) {
        Piece opponent = state.piece(pieceId);
        Position square = state.board().positionOf(PlayerColor.RED, ownStep);
        int step = state.board().route(opponent.color()).indexOf(square);
        assertTrue(step >= 0);
        opponent.moveTo(step);
    }

    private static final class ScriptedRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final int[] tickets;
        private int index;
        ScriptedRandom(int... tickets) { this.tickets = tickets.clone(); }
        @Override public int nextInt(int bound) {
            assertEquals(100, bound);
            assertTrue(index < tickets.length, "Unexpected extra surprise draw");
            return tickets[index++];
        }
    }
}

