package Model;

/** One source of truth for difficulty-specific board and dice settings (NFR-05). */
public enum Difficulty {
    EASY("קל", 15, 52, 5, 4, 4, MultiplierDie.NONE, 0),
    MEDIUM("בינוני", 17, 116, 5, 8, 8, MultiplierDie.BOOST, 2);

    private final String displayName;
    private final int boardSize;
    private final int trackSquares;
    private final int homePathSquares;
    private final int arrowSquares;
    private final int safeSquares;
    private final MultiplierDie multiplierDie;
    private final int questionSquaresPerLevel;

    /** Store level-specific settings without scattering constants across the game. */
    Difficulty(String displayName, int boardSize, int trackSquares,
            int homePathSquares, int arrowSquares, int safeSquares,
            MultiplierDie multiplierDie, int questionSquaresPerLevel) {
        this.displayName = displayName;
        this.boardSize = boardSize;
        this.trackSquares = trackSquares;
        this.homePathSquares = homePathSquares;
        this.arrowSquares = arrowSquares;
        this.safeSquares = safeSquares;
        this.multiplierDie = multiplierDie;
        this.questionSquaresPerLevel = questionSquaresPerLevel;
    }

    /** Return the Hebrew level name. */
    public String displayName() { return displayName; }
    /** Return the physical board size. */
    public int boardSize() { return boardSize; }
    /** Return the shared-track square count. */
    public int trackSquares() { return trackSquares; }
    /** Return the five private home-lane squares. */
    public int homePathSquares() { return homePathSquares; }
    /** Return the arrow square count. */
    public int arrowSquares() { return arrowSquares; }
    /** Return the safe square count. */
    public int safeSquares() { return safeSquares; }
    /** Return the multiplier die for this level. */
    public MultiplierDie multiplierDie() { return multiplierDie; }
    /** Return the required number of question cells for each question level. */
    public int questionSquaresPerLevel() { return questionSquaresPerLevel; }
    /** Display a localized difficulty name. */
    @Override public String toString() { return displayName; }
}
