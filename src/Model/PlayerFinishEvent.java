package Model;

import java.util.Objects;

/** Immutable notification that a player finished all four pieces and earned a rank (FR-45). */
public record PlayerFinishEvent(int playerIndex, String playerName, PlayerColor color, int rank) {
    /** Validate the information used to announce a completed player. */
    public PlayerFinishEvent {
        if (playerIndex < 0 || rank < 1) throw new IllegalArgumentException("Invalid player finish event");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(color, "color");
    }
}
