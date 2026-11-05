package Model;

import java.util.*;

/** Finds the legal moves for a roll: FR-23, FR-24, FR-26, FR-27, FR-31 (step count: FR-16, FR-17). */
public final class MoveValidator {
    /** Prevent direct instantiation of the validator. */
    private MoveValidator() { }

    /** Offer full legal moves first, otherwise allowed moves stopping before a block. */
    public static List<Move> legalMoves(GameState state, DiceRoll roll) {
        Board board = state.board();
        Player player = state.currentPlayer();
        List<Move> full = new ArrayList<>();
        List<Move> partial = new ArrayList<>();
        for (Piece piece : state.piecesOf(player)) {
            if (piece.isInCastle()) {
                releaseMove(state, piece, roll).ifPresent(full::add);
                continue;
            }
            if (state.isFinished(piece)) continue;
            int target = piece.step() + stepsFor(piece, roll, board);
            if (target > board.finishStep()) continue; // FR-26: the finish needs an exact roll
            int reachable = furthestReachable(state, piece, target);
            if (reachable == target) {
                full.add(new Move(piece.id(), piece.step(), target, false, false));
            } else if (reachable > piece.step()) {
                partial.add(new Move(piece.id(), piece.step(), reachable, false, true));
            }
        }
        return List.copyOf(full.isEmpty() ? partial : full);
    }

    /** Release a pawn only on six and only when its entry is not blocked. */
    static Optional<Move> releaseMove(GameState state, Piece piece, DiceRoll roll) {
        if (!roll.isSix()) return Optional.empty();
        Position start = state.board().positionOf(piece.color(), 0);
        if (InteractionRules.hasOpponentDoubleBlock(state, piece.ownerIndex(), start)) return Optional.empty();
        return Optional.of(new Move(piece.id(), Piece.IN_CASTLE, 0, true, false));
    }

    /** Use the ordinary die result for Easy-level movement. */
    static int stepsFor(Piece piece, DiceRoll roll, Board board) {
        return roll.value();
    }

    /** Walks square by square towards the target and stops before the first opponent Double Block (FR-31). */
    static int furthestReachable(GameState state, Piece piece, int target) {
        Board board = state.board();
        int direction = target >= piece.step() ? 1 : -1;
        int step = piece.step();
        while (step != target) {
            int next = step + direction;
            Position square = board.positionOf(piece.color(), next);
            if (InteractionRules.hasOpponentDoubleBlock(state, piece.ownerIndex(), square)) break;
            step = next;
        }
        return step;
    }
}
