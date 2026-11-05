package Model;

import java.util.Objects;

/** A participant: name, unique colour and, once ranked, the final place (FR-03, FR-45). */
public final class Player {
    private final int index;
    private final String name;
    private final PlayerColor color;
    private int rank;
    private boolean completedAllPieces;

    Player(int index, String name, PlayerColor color) {
        this.index = index;
        this.name = Objects.requireNonNull(name);
        this.color = Objects.requireNonNull(color);
    }

    /** Position of the player in the turn order (0-based). */
    public int index() { return index; }
    public String name() { return name; }
    public PlayerColor color() { return color; }
    /** Final place, or 0 while the player is still playing. */
    public int rank() { return rank; }
    public boolean isFinished() { return rank > 0; }
    /** False only for the player left last without bringing all pieces home (FR-47). */
    public boolean completedAllPieces() { return completedAllPieces; }

    void assignRank(int rank, boolean completedAllPieces) {
        this.rank = rank;
        this.completedAllPieces = completedAllPieces;
    }
}
