package Model;

/**
 * Centralized difficulty-specific board settings (FR-07–FR-14)
 * and multiplier die selection (FR-15).
 */
public enum Difficulty {
    EASY("קל", 15, 52, 5, 4, 4, 0, MultiplierDie.NONE, 0),
    MEDIUM("בינוני", 17, 116, 5, 8, 8, 2, MultiplierDie.BOOST, 2);

    private final String displayName;
    private final int boardSize;
    private final int trackSquares;
    private final int homePathSquares;
    private final int arrowSquares;
    private final int safeSquares;
    private final int surpriseSquares;
    private final MultiplierDie multiplierDie;
    private final int questionSquaresPerLevel;

    Difficulty(String displayName, int boardSize, int trackSquares, int homePathSquares, int arrowSquares, int safeSquares, int surpriseSquares, MultiplierDie multiplierDie, int questionSquaresPerLevel) {
        this.displayName = displayName;
        this.boardSize = boardSize;
        this.trackSquares = trackSquares;
        this.homePathSquares = homePathSquares;
        this.arrowSquares = arrowSquares;
        this.safeSquares = safeSquares;
        this.surpriseSquares = surpriseSquares;
        this.multiplierDie = multiplierDie;
        this.questionSquaresPerLevel = questionSquaresPerLevel;
    }

    public String displayName() { return displayName; }
    public int boardSize() { return boardSize; }
    public int trackSquares() { return trackSquares; }
    public int homePathSquares() { return homePathSquares; }
    public int arrowSquares() { return arrowSquares; }
    public int safeSquares() { return safeSquares; }
    /** Number of randomly placed surprise squares (FR-13). */
    public int surpriseSquares() { return surpriseSquares; }
    public int questionSquaresPerLevel() { return questionSquaresPerLevel; }
    public MultiplierDie multiplierDie() { return multiplierDie; }

    @Override
    public String toString() { return displayName; }
}