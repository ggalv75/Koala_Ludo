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

    Difficulty(String displayName, int boardSize, int trackSquares,
               int homePathSquares, int arrowSquares, int safeSquares) {
        this.displayName = displayName;
        this.boardSize = boardSize;
        this.trackSquares = trackSquares;
        this.homePathSquares = homePathSquares;
        this.arrowSquares = arrowSquares;
        this.safeSquares = safeSquares;
    }

    public String displayName() { return displayName; }
    /** Number of rows (and columns) of the square board grid. */
    public int boardSize() { return boardSize; }
    /** Squares of the shared track around the board. */
    public int trackSquares() { return trackSquares; }
    /**
     * Coloured squares of each home path. Together with the arrow square that leads into it,
     * a home path is six coloured squares long, as in the Easy board diagram (FR-09).
     */
    public int homePathSquares() { return homePathSquares; }
    public int arrowSquares() { return arrowSquares; }
    public int safeSquares() { return safeSquares; }

    @Override
    public String toString() { return displayName; }
}
