package Model;

/**
 * Difficulty-specific board settings and multiplier dice.
 */
public enum Difficulty {
    EASY("קל", 15, 52, 5, 4, 4, MultiplierDie.NONE),
    MEDIUM("בינוני", 17, 116, 5, 8, 8, MultiplierDie.BOOST);

    private final String displayName;
    private final int boardSize;
    private final int trackSquares;
    private final int homePathSquares;
    private final int arrowSquares;
    private final int safeSquares;
    private final MultiplierDie multiplierDie;

    Difficulty(String displayName, int boardSize, int trackSquares, int homePathSquares, int arrowSquares, int safeSquares, MultiplierDie multiplierDie) {
        this.displayName = displayName;
        this.boardSize = boardSize;
        this.trackSquares = trackSquares;
        this.homePathSquares = homePathSquares;
        this.arrowSquares = arrowSquares;
        this.safeSquares = safeSquares;
        this.multiplierDie = multiplierDie;
    }

    public String displayName() { return displayName; }
    public int boardSize() { return boardSize; }
    public int trackSquares() { return trackSquares; }
    public int homePathSquares() { return homePathSquares; }
    public int arrowSquares() { return arrowSquares; }
    public int safeSquares() { return safeSquares; }
    public MultiplierDie multiplierDie() { return multiplierDie; }

    @Override
    public String toString() { return displayName; }
}