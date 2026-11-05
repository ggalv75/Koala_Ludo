package Model;

/** Square types used at the Easy level (Ludo Project-V2, pp. 2-3). */
public enum CellType {
    REGULAR,
    /** FR-08: a player's entry square, marked with #. */
    START,
    /** FR-10: leads a piece of the same colour into its home path. */
    ARROW,
    /** FR-11: marked with a star. */
    SAFE,
    /** FR-09: a square of a coloured home path. */
    HOME;

    /** FR-08, FR-09, FR-29: pieces on start, safe and home squares cannot be captured. */
    public boolean isProtected() {
        return this == START || this == SAFE || this == HOME;
    }
}
