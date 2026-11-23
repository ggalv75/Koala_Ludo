package Model;

import java.util.Objects;
import java.util.Optional;

/** Result of a surprise strategy, consumed by the engine's existing flow. */
public record SurpriseResolution(
        Optional<Move> movement,
        boolean requiresOpponentSelection) {

    public SurpriseResolution {
        Objects.requireNonNull(movement, "movement");

        if (requiresOpponentSelection && movement.isPresent()) {
            throw new IllegalArgumentException(
                    "A strategy cannot move a piece and request selection together");
        }
    }

    public static SurpriseResolution movement(Optional<Move> move) {
        return new SurpriseResolution(move, false);
    }

    public static SurpriseResolution none() {
        return new SurpriseResolution(Optional.empty(), false);
    }

    public static SurpriseResolution opponentSelection() {
        return new SurpriseResolution(Optional.empty(), true);
    }
}