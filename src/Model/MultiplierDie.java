package Model;

/**
 * The multiplier die rolled next to the regular step die (Ludo Project-V2, p. 4; SRS FR-15).
 * Boost has faces 1-3 (Medium level), UltraBoost has faces 1-6 (Hard level and the
 * UltraBoost surprise at every level, FR-21). {@link #NONE} means only the step die is rolled (Easy).
 */
public enum MultiplierDie {
    NONE("", 0),
    BOOST("Boost", 3),
    ULTRA_BOOST("UltraBoost", 6);

    private final String displayName;
    private final int faces;

    /** Store the name shown to the player and the number of faces. */
    MultiplierDie(String displayName, int faces) {
        this.displayName = displayName;
        this.faces = faces;
    }

    /** Name shown on the board and in the game log. */
    public String displayName() { return displayName; }

    /** Highest face of the die; 0 when there is no multiplier die. */
    public int faces() { return faces; }

    /** Check that a value can come up on this die (0 is the only value of {@link #NONE}). */
    public boolean accepts(int value) {
        return this == NONE ? value == 0 : value >= 1 && value <= faces;
    }
}
