package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SurpriseStrategyTest {

    private GameState state;

    private final SurpriseStrategies strategies =
            new SurpriseStrategies();

    @BeforeEach
    void setUp() {
        state = new GameEngine(
                Difficulty.EASY,
                List.of("Dor", "Gal"),
                List.of(
                        PlayerColor.RED,
                        PlayerColor.GREEN))
                .state();

        state.piece(0).moveTo(10);
    }

    @Test
    void directFinishStrategyPlansAJumpWithoutChangingThePiece() {
        SurpriseResolution result = strategies
                .forType(SurpriseType.DIRECT_FINISH)
                .apply(state, 0);

        Move move = result.movement().orElseThrow();

        assertEquals(
                state.board().finishStep(),
                move.toStep());

        assertEquals(10, state.piece(0).step());
        assertFalse(result.requiresOpponentSelection());
    }

    @Test
    void castleStrategyPlansAReturnWithoutChangingThePiece() {
        SurpriseResolution result = strategies
                .forType(SurpriseType.RETURN_TO_CASTLE)
                .apply(state, 0);

        assertEquals(
                Piece.IN_CASTLE,
                result.movement().orElseThrow().toStep());

        assertEquals(10, state.piece(0).step());
    }

    @Test
    void backwardStrategyUsesTheExistingBlockRules() {
        Position block = state.board()
                .positionOf(PlayerColor.RED, 8);

        int opponentStep = state.board()
                .route(PlayerColor.GREEN)
                .indexOf(block);

        assertTrue(opponentStep >= 0);

        state.piece(4).moveTo(opponentStep);
        state.piece(5).moveTo(opponentStep);

        Move move = strategies
                .forType(SurpriseType.MOVE_BACKWARD_THREE)
                .apply(state, 0)
                .movement()
                .orElseThrow();

        assertEquals(9, move.toStep());
        assertTrue(move.partial());
        assertEquals(10, state.piece(0).step());
    }

    @Test
    void opponentStrategyRequestsSelectionWithoutChoosingOrMovingAPawn() {
        SurpriseResolution result = strategies
                .forType(SurpriseType.MOVE_OPPONENT_BACKWARD)
                .apply(state, 0);

        assertTrue(result.requiresOpponentSelection());
        assertTrue(result.movement().isEmpty());
        assertEquals(10, state.piece(0).step());
        assertTrue(state.piece(4).isInCastle());
        assertEquals(0, state.currentPlayerIndex());
    }

    @Test
    void lostTurnStrategySchedulesThePenaltyWithoutPassingTheTurn() {
        SurpriseResolution result = strategies
                .forType(SurpriseType.LOSE_NEXT_TURN)
                .apply(state, 0);

        assertTrue(
                state.losesNextTurn(state.currentPlayer()));

        assertTrue(result.movement().isEmpty());
        assertEquals(0, state.currentPlayerIndex());
    }

    @Test
    void ultraBoostStrategySchedulesTheExistingSpecialThrow() {
        SurpriseResolution result = strategies
                .forType(SurpriseType.ULTRA_BOOST_ROLL)
                .apply(state, 0);

        assertTrue(state.specialRollPending());

        assertEquals(
                MultiplierDie.ULTRA_BOOST,
                state.nextThrowMultiplier());

        assertTrue(result.movement().isEmpty());
        assertEquals(0, state.currentPlayerIndex());
    }

    @Test
    void engineUsesAnInjectedStrategyInsteadOfAHardCodedSurpriseEffect() {
        Board board = BoardFactory.create(
                Difficulty.MEDIUM,
                new Random(42));

        int target = -1;

        for (int step = 1;
                step < board.homePathStart();
                step++) {

            if (board.cellAt(PlayerColor.RED, step).type()
                    == CellType.SURPRISE) {

                target = step;
                break;
            }
        }

        assertTrue(target > 0,
                "The Medium board must contain a surprise on this route");

        Map<SurpriseType, SurpriseStrategy> policies =
                new EnumMap<>(SurpriseType.class);

        for (SurpriseType type : SurpriseType.values()) {
            policies.put(
                    type, strategies.forType(type));
        }

        AtomicInteger calls = new AtomicInteger();

        policies.put(
                SurpriseType.DIRECT_FINISH,
                (gameState, pieceId) -> {
                    calls.incrementAndGet();

                    return SurpriseResolution.movement(
                            SurpriseMovement.returnToCastle(
                                    gameState, pieceId));
                });

        Random draw = new Random() {
            private static final long serialVersionUID = 1L;

            @Override
            public int nextInt(int bound) {
                assertEquals(100, bound);

                // Select DIRECT_FINISH with a replacement strategy.
                return 0;
            }
        };

        Dice dice = new Dice() {
            @Override
            public DiceRoll roll() {
                return new DiceRoll(1);
            }

            @Override
            public int rollMultiplier(MultiplierDie die) {
                assertEquals(
                        MultiplierDie.BOOST, die);

                return 2;
            }
        };

        GameEngine engine = new GameEngine(
                board,
                List.of("Dor", "Gal"),
                List.of(
                        PlayerColor.RED,
                        PlayerColor.GREEN),
                dice,
                Clock.systemUTC(),
                new SurpriseSelector(draw),
                new SurpriseStrategies(policies));

        engine.state().piece(0)
                .moveTo(target - 1);

        engine.rollDice();

        assertTrue(engine.movePiece(0));

        assertEquals(1, calls.get());

        assertTrue(
                engine.state().piece(0).isInCastle(),
                "The engine must execute the replacement strategy");

        assertEquals(
                1, engine.state().currentPlayerIndex());
    }
}