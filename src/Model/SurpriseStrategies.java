package Model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Selects a Strategy for a drawn surprise.
 * The six concrete strategies are nested classes in this file.
 */
public final class SurpriseStrategies {

    private final Map<SurpriseType, SurpriseStrategy> strategies;

    /** The standard family of six surprise strategies. */
    public SurpriseStrategies() {
        this(Map.of(
                SurpriseType.DIRECT_FINISH,
                new DirectFinishStrategy(),

                SurpriseType.MOVE_OPPONENT_BACKWARD,
                new MoveOpponentBackwardStrategy(),

                SurpriseType.RETURN_TO_CASTLE,
                new ReturnToCastleStrategy(),

                SurpriseType.MOVE_BACKWARD_THREE,
                new MoveBackwardThreeStrategy(),

                SurpriseType.LOSE_NEXT_TURN,
                new LoseNextTurnStrategy(),

                SurpriseType.ULTRA_BOOST_ROLL,
                new UltraBoostRollStrategy()));
    }

    /** Inject alternative policies without changing the engine's control flow. */
    public SurpriseStrategies(
            Map<SurpriseType, SurpriseStrategy> strategies) {

        Objects.requireNonNull(strategies, "strategies");

        Map<SurpriseType, SurpriseStrategy> copy =
                new EnumMap<>(SurpriseType.class);

        copy.putAll(strategies);

        for (SurpriseType type : SurpriseType.values()) {
            if (!copy.containsKey(type)) {
                throw new IllegalArgumentException(
                        "Missing strategy: " + type);
            }

            Objects.requireNonNull(
                    copy.get(type), "strategy for " + type);
        }

        this.strategies = Map.copyOf(copy);
    }

    public SurpriseStrategy forType(SurpriseType type) {
        return strategies.get(
                Objects.requireNonNull(type, "type"));
    }

    /** FR-39: direct finish bypasses the ordinary path and blocks. */
    public static final class DirectFinishStrategy
            implements SurpriseStrategy {

        @Override
        public SurpriseResolution apply(
                GameState state, int pieceId) {

            return SurpriseResolution.movement(
                    SurpriseMovement.directFinish(state, pieceId));
        }
    }

    /** FR-40: the engine waits for a legal opponent selection before rolling. */
    public static final class MoveOpponentBackwardStrategy
            implements SurpriseStrategy {

        @Override
        public SurpriseResolution apply(
                GameState state, int pieceId) {

            return SurpriseResolution.opponentSelection();
        }
    }

    /** FR-41: return the landing piece to its castle. */
    public static final class ReturnToCastleStrategy
            implements SurpriseStrategy {

        @Override
        public SurpriseResolution apply(
                GameState state, int pieceId) {

            return SurpriseResolution.movement(
                    SurpriseMovement.returnToCastle(state, pieceId));
        }
    }

    /** FR-42: move backward, respecting the start and opponent blocks. */
    public static final class MoveBackwardThreeStrategy
            implements SurpriseStrategy {

        @Override
        public SurpriseResolution apply(
                GameState state, int pieceId) {

            return SurpriseResolution.movement(
                    SurpriseMovement.backward(state, pieceId, 3));
        }
    }

    /** FR-43: use the existing turn rules for the active player. */
    public static final class LoseNextTurnStrategy
            implements SurpriseStrategy {

        @Override
        public SurpriseResolution apply(
                GameState state, int pieceId) {

            TurnManager.loseNextTurn(
                    state, state.currentPlayer());

            return SurpriseResolution.none();
        }
    }

    /** FR-44: schedule the special throw implemented by the dice subsystem. */
    public static final class UltraBoostRollStrategy
            implements SurpriseStrategy {

        @Override
        public SurpriseResolution apply(
                GameState state, int pieceId) {

            TurnManager.grantSpecialRoll(state);

            return SurpriseResolution.none();
        }
    }
}