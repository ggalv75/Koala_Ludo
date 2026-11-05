package Model;

/** The four player colours, in clockwise board order. Screen colours are defined in the View (View.Theme). */
public enum PlayerColor {
    RED("אדום"),
    GREEN("ירוק"),
    YELLOW("צהוב"),
    BLUE("כחול");

    private final String displayName;

    PlayerColor(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
