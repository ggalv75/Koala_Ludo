package Model;

/**
 * Single place for every parameter that differs between difficulty levels (SRS NFR-05).
 * Iteration 1 implements the EASY level only (SDP 5.2.1). MEDIUM and HARD are added here
 * in iterations 2-3, together with their extra tracks, special dice and special squares.
 */
public enum Difficulty {
    /** FR-07, FR-09, FR-10, FR-11: 15x15 board, 52-square track, 4 arrows, 4 safe squares, regular die only. */
    EASY("קל", 15, 52, 5, 4, 4);

    private final String displayName;
    private final int boardSize;
    private final int trackSquares;
    private final int homePathSquares;
    private final int arrowSquares;
    private final int safeSquares;

    /** Store the board dimensions and square counts for one difficulty. */
    Difficulty(String displayName, int boardSize, int trackSquares,
               int homePathSquares, int arrowSquares, int safeSquares) {
        this.displayName = displayName;
        this.boardSize = boardSize;
        this.trackSquares = trackSquares;
        this.homePathSquares = homePathSquares;
        this.arrowSquares = arrowSquares;
        this.safeSquares = safeSquares;
    }

    /** Return the localized name of the selected difficulty. */
    public String displayName() { return displayName; }
    /** Number of rows (and columns) of the square board grid. */
    public int boardSize() { return boardSize; }
    /** Squares of the shared track around the board. */
    public int trackSquares() { return trackSquares; }
    /** Return the five home squares that combine with the arrow into a six-square colored path. */
    public int homePathSquares() { return homePathSquares; }
    /** Return the required number of entry arrows. */
    public int arrowSquares() { return arrowSquares; }
    /** Return the required number of protected safe squares. */
    public int safeSquares() { return safeSquares; }

    /** Show the localized difficulty name in UI controls. */
    @Override
    public String toString() { return displayName; }
}
