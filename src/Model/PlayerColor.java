package Model;

/** The four player colours, in clockwise board order. Screen colours are defined in the View (View.Theme). */
public enum PlayerColor {
    RED("אדום"),
    GREEN("ירוק"),
    YELLOW("צהוב"),
    BLUE("כחול");

    private final String displayName;

    /** Bind the player color to its localized label. */
    PlayerColor(String displayName) { this.displayName = displayName; }

    /** Return the localized label for the player color. */
    public String displayName() { return displayName; }

    /** Show the localized color name in UI controls. */
    @Override
    public String toString() { return displayName; }
}
