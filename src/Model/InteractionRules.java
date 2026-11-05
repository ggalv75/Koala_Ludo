package Model;

import java.util.*;

/** Interactions between pieces: capture (FR-28), protection (FR-29) and Double Block (FR-30, FR-32). */
public final class InteractionRules {
    private InteractionRules() { }

    /** Pieces standing on the given square (pieces in a castle or the finish area are never on a square). */
    public static List<Piece> piecesAt(GameState state, Position square) {
        List<Piece> found = new ArrayList<>();
        if (square == null) return found;
        for (Piece piece : state.pieces()) {
            if (square.equals(state.positionOf(piece))) found.add(piece);
        }
        return found;
    }

    /**
     * FR-30: two or more pieces of the same player on one square form a Double Block, on any square type.
     * FR-32: the block exists only while at least two of them remain, so it is recomputed every time.
     */
    public static boolean isDoubleBlock(GameState state, Position square) {
        return blockOwner(state, square).isPresent();
    }

    /** True when an opponent of {@code movingOwner} holds a Double Block on the square. */
    public static boolean hasOpponentDoubleBlock(GameState state, int movingOwner, Position square) {
        Optional<Integer> owner = blockOwner(state, square);
        return owner.isPresent() && owner.get() != movingOwner;
    }

    private static Optional<Integer> blockOwner(GameState state, Position square) {
        Map<Integer, Integer> perOwner = new HashMap<>();
        for (Piece piece : piecesAt(state, square)) perOwner.merge(piece.ownerIndex(), 1, Integer::sum);
        return perOwner.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * FR-28 / FR-29: a piece that lands on an unprotected square holding exactly one opponent piece
     * captures it. Start, safe and home squares are protected, so pieces there can share the square.
     */
    public static Optional<Piece> capturableOpponent(GameState state, Piece arriving) {
        Position square = state.positionOf(arriving);
        if (square == null) return Optional.empty();
        Cell cell = state.board().cellAt(square);
        if (cell == null || cell.isProtected()) return Optional.empty();
        List<Piece> opponents = new ArrayList<>();
        for (Piece piece : piecesAt(state, square)) {
            if (piece.ownerIndex() != arriving.ownerIndex()) opponents.add(piece);
        }
        return opponents.size() == 1 ? Optional.of(opponents.get(0)) : Optional.empty();
    }
}
