package Model;

/**
 * Single place for every parameter that differs between difficulty levels (SRS NFR-05).
 * Iteration 1 implements the EASY level only (SDP 5.2.1). MEDIUM and HARD are added here
 * in iterations 2-3, together with their extra tracks, special dice and special squares.
 * The multiplier die per level (FR-15): EASY - none, MEDIUM - {@link MultiplierDie#BOOST},
 * HARD - {@link MultiplierDie#ULTRA_BOOST}.
 */
public enum Difficulty {
    /** FR-07, FR-09, FR-10, FR-11: 15x15 board, 52-square track, 4 arrows, 4 safe squares, regular die only. */
    EASY("קל", 15, 52, 5, 4, 4, MultiplierDie.NONE);

    private final String displayName;
    private final int boardSize;
    private final int trackSquares;
    private final int homePathSquares;
    private final int arrowSquares;
    private final int safeSquares;
    private final MultiplierDie multiplierDie;

    /** Store the board dimensions, square counts and multiplier die for one difficulty. */
    Difficulty(String displayName, int boardSize, int trackSquares,
               int homePathSquares, int arrowSquares, int safeSquares, MultiplierDie multiplierDie) {
        this.displayName = displayName;
        this.boardSize = boardSize;
        this.trackSquares = trackSquares;
        this.homePathSquares = homePathSquares;
        this.arrowSquares = arrowSquares;
        this.safeSquares = safeSquares;
        this.multiplierDie = multiplierDie;
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
    /** FR-15: the multiplier die rolled with the step die at this level ({@link MultiplierDie#NONE} at Easy). */
    public MultiplierDie multiplierDie() { return multiplierDie; }

    /** Show the localized difficulty name in UI controls. */
    @Override
    public String toString() { return displayName; }
}
