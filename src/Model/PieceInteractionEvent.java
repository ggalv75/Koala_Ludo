package Model;

import java.util.List;
import java.util.Objects;

/** Immutable result of one legal move, used only to present FR-28–FR-32 effects. */
public record PieceInteractionEvent(
        Capture capture,
        Position protectedLanding,
        Position blockedBy,
        List<BlockChange> blockChanges) {

    /** The captured piece and the square on which capture occurred. */
    public record Capture(int pieceId, Position square) { }

    /** A newly formed or removed Double Block for one player at one square. */
    public record BlockChange(int ownerIndex, Position square, boolean formed) { }

    /** Copy the changes defensively so event details cannot change later. */
    public PieceInteractionEvent {
        blockChanges = List.copyOf(Objects.requireNonNull(blockChanges));
    }

    /** Indicate whether this move has any interaction effect to show. */
    public boolean hasVisualEffect() {
        return capture != null || protectedLanding != null || blockedBy != null || !blockChanges.isEmpty();
    }
}
