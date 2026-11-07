package Model;

/** One of a player's four pieces. Its location is a step on the owner's route (see {@link Board}). */
public final class Piece {
    public static final int IN_CASTLE = -1;

    private final int id;
    private final int ownerIndex;
    private final int number;
    private final PlayerColor color;
    private int step = IN_CASTLE;

    /** Create one numbered piece in its owner’s castle. */
    Piece(int id, int ownerIndex, int number, PlayerColor color) {
        this.id = id;
        this.ownerIndex = ownerIndex;
        this.number = number;
        this.color = color;
    }

    /** Return the stable identifier of this piece. */
    public int id() { return id; }
    /** Return the owning player’s index. */
    public int ownerIndex() { return ownerIndex; }
    /** 1-4, the number shown on the piece. */
    public int number() { return number; }
    /** Return the player color of this piece. */
    public PlayerColor color() { return color; }
    /** Return the piece’s current step along its route. */
    public int step() { return step; }
    /** Check whether this piece remains in its starting castle. */
    public boolean isInCastle() { return step == IN_CASTLE; }

    /** Update the route step after a validated move. */
    void moveTo(int step) { this.step = step; }
    /** Reset a captured piece to its starting castle. */
    void returnToCastle() { this.step = IN_CASTLE; }
}
