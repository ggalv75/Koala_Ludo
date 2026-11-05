package Model;

/** A square type plus its owning colour (for start, arrow and home squares; null otherwise). */
public record Cell(CellType type, PlayerColor color) {
    static final Cell REGULAR = new Cell(CellType.REGULAR, null);

    public boolean isProtected() { return type.isProtected(); }
}
