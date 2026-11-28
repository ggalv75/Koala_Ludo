package Model;

import java.util.Objects;
import java.util.Optional;

/** Calculates surprise movement without changing the game state. */
public final class SurpriseMovement {

    private SurpriseMovement() { }

    /**
     * Calculate backward movement, stopping at the start square or
     * immediately before an opponent's Double Block.
     * Castle and finished pieces cannot move backward.
     */
    public static Optional<Move> backward(
            GameState state, int pieceId, int steps) {

        Objects.requireNonNull(state, "state");

        if (steps <= 0) {
            throw new IllegalArgumentException("Steps must be positive");
        }

        Piece piece = state.piece(pieceId);

        if (piece.isInCastle() || state.isFinished(piece)) {
            return Optional.empty();
        }

        int target = Math.max(0, piece.step() - steps);
        int reachable = MoveValidator.furthestReachable(
                state, piece, target);

        if (reachable == piece.step()) {
            return Optional.empty();
        }

        return Optional.of(new Move(
                piece.id(),
                piece.step(),
                reachable,
                false,
                reachable != target));
    }

    /**
     * Calculate a direct move to the finish.
     * This surprise bypasses blocks and the exact-step requirement.
     */
    public static Optional<Move> directFinish(
            GameState state, int pieceId) {

        Objects.requireNonNull(state, "state");
        Piece piece = state.piece(pieceId);

        if (piece.isInCastle() || state.isFinished(piece)) {
            return Optional.empty();
        }

        return Optional.of(new Move(
                piece.id(),
                piece.step(),
                state.board().finishStep(),
                false,
                false));
    }

    /** Calculate a return to the castle for an active piece. */
    public static Optional<Move> returnToCastle(
            GameState state, int pieceId) {

        Objects.requireNonNull(state, "state");
        Piece piece = state.piece(pieceId);

        if (piece.isInCastle() || state.isFinished(piece)) {
            return Optional.empty();
        }

        return Optional.of(new Move(
                piece.id(),
                piece.step(),
                Piece.IN_CASTLE,
                false,
                false));
    }
}