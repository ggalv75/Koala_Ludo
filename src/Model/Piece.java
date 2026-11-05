package Model;

/** One of a player's four pieces. Its location is a step on the owner's route (see {@link Board}). */
public final class Piece {
    public static final int IN_CASTLE = -1;

    private final int id;
    private final int ownerIndex;
    private final int number;
    private final PlayerColor color;
    private int step = IN_CASTLE;

    Piece(int id, int ownerIndex, int number, PlayerColor color) {
        this.id = id;
        this.ownerIndex = ownerIndex;
        this.number = number;
        this.color = color;
    }

    public int id() { return id; }
    public int ownerIndex() { return ownerIndex; }
    /** 1-4, the number shown on the piece. */
    public int number() { return number; }
    public PlayerColor color() { return color; }
    public int step() { return step; }
    public boolean isInCastle() { return step == IN_CASTLE; }

    void moveTo(int step) { this.step = step; }
    void returnToCastle() { this.step = IN_CASTLE; }
}
