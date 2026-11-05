package Model;

import java.util.*;

/** Centralizes capture, protected squares, and Double Block rules (FR-28–FR-32). */
public final class InteractionRules {
    /** Prevent construction of this rules utility. */
    private InteractionRules() { }

    /** Return all on-board pieces occupying the physical square. */
    public static List<Piece> piecesAt(GameState state, Position square) {
        Objects.requireNonNull(state);
        if (square == null) return List.of();
        List<Piece> found = new ArrayList<>();
        for (Piece piece : state.pieces()) {
            if (square.equals(state.positionOf(piece))) found.add(piece);
        }
        return List.copyOf(found);
    }

    /** Return every owner with two or more pieces on the same square (FR-30/FR-32). */
    public static Set<Integer> blockOwners(GameState state, Position square) {
        if (square == null) return Set.of();
        Map<Integer, Integer> counts = new HashMap<>();
        for (Piece piece : piecesAt(state, square)) {
            counts.merge(piece.ownerIndex(), 1, Integer::sum);
        }
        Set<Integer> owners = new TreeSet<>();
        counts.forEach((owner, count) -> {
            if (count >= 2) owners.add(owner);
        });
        return Collections.unmodifiableSet(owners);
    }

    /** Detect whether at least one Double Block occupies the square (FR-30). */
    public static boolean isDoubleBlock(GameState state, Position square) {
        return !blockOwners(state, square).isEmpty();
    }

    /** Reject landing on or crossing any opponent's Double Block (FR-30/FR-31). */
    public static boolean hasOpponentDoubleBlock(GameState state, int movingOwner, Position square) {
        return blockOwners(state, square).stream().anyMatch(owner -> owner != movingOwner);
    }

    /** Find the sole capturable opposing piece on an unprotected square (FR-28/FR-29). */
    public static Optional<Piece> capturableOpponent(GameState state, Piece arriving) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(arriving);
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

    /** Detect a protected landing shared with an opponent without capturing it (FR-29). */
    public static boolean sharesProtectedSquareWithOpponent(GameState state, Piece arriving) {
        Position square = state.positionOf(arriving);
        if (square == null) return false;
        Cell cell = state.board().cellAt(square);
        return cell != null && cell.isProtected() && piecesAt(state, square).stream()
                .anyMatch(piece -> piece.ownerIndex() != arriving.ownerIndex());
    }
}
