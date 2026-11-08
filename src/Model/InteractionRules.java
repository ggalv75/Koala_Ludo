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

	/** Count pieces belonging to each player on a given square. */
	private static Map<Integer, Integer> countPiecesByOwner(GameState state, Position square) {
	    if (square == null) return Map.of();
	
	    Map<Integer, Integer> counts = new HashMap<>();
	    for (Piece piece : state.pieces()) {
	        if (square.equals(state.positionOf(piece))) {
	            counts.merge(piece.ownerIndex(), 1, Integer::sum);
	        }
	    }
	    return counts;
	}
	
	/** Return all players with at least two pieces on the same square. */
	public static Set<Integer> blockOwners(GameState state, Position square) {
	    Map<Integer, Integer> counts = countPiecesByOwner(state, square);
	    Set<Integer> owners = new TreeSet<>();
	
	    counts.forEach((owner, count) -> {
	        if (count >= 2) owners.add(owner);
	    });
	
	    return Collections.unmodifiableSet(owners);
	}
	
	/** Check whether a square contains any Double Block. */
	public static boolean isDoubleBlock(GameState state, Position square) {
	    return countPiecesByOwner(state, square).values().stream()
	            .anyMatch(count -> count >= 2);
	}
	
	/** Check whether an opponent has a Double Block on the square. */
	public static boolean hasOpponentDoubleBlock(GameState state, int movingOwner, Position square) {
	    return countPiecesByOwner(state, square).entrySet().stream()
	            .anyMatch(entry -> entry.getKey() != movingOwner && entry.getValue() >= 2);
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
