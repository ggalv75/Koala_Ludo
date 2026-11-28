package Model;

import java.util.Objects;

/**
 * Immutable notification that a surprise was drawn (FR-38 to FR-44).
 * Identifies the triggering player and piece, not a chosen opponent.
 * This event announces the draw; it does not confirm effect completion.
 */
public record SurpriseEvent(
        int playerIndex,
        int pieceId,
        SurpriseType type) {

    /** Validate the event data before notifying observers. */
    public SurpriseEvent {
        if (playerIndex < 0) {
            throw new IllegalArgumentException(
                    "Player index must not be negative");
        }

        if (pieceId < 0) {
            throw new IllegalArgumentException(
                    "Piece ID must not be negative");
        }

        Objects.requireNonNull(type, "type");
    }
}