package Model;

import java.util.Objects;

/** A participant: name, unique colour and, once ranked, the final place (FR-03, FR-45). */
public final class Player {
    private final int index;
    private final String name;
    private final PlayerColor color;
    private int rank;
    private boolean completedAllPieces;

    /** Create a player with a stable index, name and unique color. */
    Player(int index, String name, PlayerColor color) {
        this.index = index;
        this.name = Objects.requireNonNull(name);
        this.color = Objects.requireNonNull(color);
    }

    /** Position of the player in the turn order (0-based). */
    public int index() { return index; }
    /** Return this player’s display name. */
    public String name() { return name; }
    /** Return the unique color assigned to this player. */
    public PlayerColor color() { return color; }
    /** Final place, or 0 while the player is still playing. */
    public int rank() { return rank; }
    /** Check whether this player has already received a rank. */
    public boolean isFinished() { return rank > 0; }
    /** Report whether all four pieces finished rather than taking last place by elimination. */
    public boolean completedAllPieces() { return completedAllPieces; }

    /** Assign the finishing rank and completion status. */
    void assignRank(int rank, boolean completedAllPieces) {
        this.rank = rank;
        this.completedAllPieces = completedAllPieces;
    }
}
