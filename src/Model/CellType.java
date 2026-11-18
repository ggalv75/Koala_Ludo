package Model;

/** Square types supported by the game (Ludo Project-V2, pp. 2-3). */
public enum CellType {
    REGULAR,
    /** FR-08: a player's entry square, marked with #. */
    START,
    /** FR-10: leads a piece of the same colour into its home path. */
    ARROW,
    /** FR-11: marked with a star. */
    SAFE,
    /** FR-09: a square of a coloured home path. */
    HOME,
    /** FR-38: landing draws a surprise; the square is protected from capture. */
    SURPRISE;

    /** FR-08, FR-09, FR-29: pieces on start, safe and home squares cannot be captured. */
    public boolean isProtected() {
        return this == START || this == SAFE || this == HOME || this == SURPRISE;
    }
}

