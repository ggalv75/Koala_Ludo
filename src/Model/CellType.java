package Model;

/** Types of cells supported by the Easy and Medium Ludo boards. */
public enum CellType {
    REGULAR,
    /** Entry square for a pawn, marked with #. */
    START,
    /** Direction change into the player's home lane. */
    ARROW,
    /** Protected star square. */
    SAFE,
    /** Part of the owner's home lane. */
    HOME,
    /** FR-33: green, easy question square. */
    QUESTION_EASY,
    /** FR-33: yellow, medium question square. */
    QUESTION_MEDIUM,
    /** FR-33: red, hard question square. */
    QUESTION_HARD;

    /** Only the explicitly protected cells prevent captures. */
    public boolean isProtected() {
        return this == START || this == SAFE || this == HOME;
    }
}
