package Model;

import java.time.LocalDateTime;
import java.util.List;

/** Final outcome shown on the results screen (FR-45 to FR-49). */
public record GameResult(Difficulty difficulty, List<Standing> standings, long durationSeconds, LocalDateTime endedAt) {

    /** One line of the final ranking. */
    public record Standing(int rank, String name, PlayerColor color, boolean completedAllPieces) { }

    /** Defensively copy the list of final standings. */
    public GameResult {
        standings = List.copyOf(standings);
    }

    /** Return the first-ranked player in the results. */
    public Standing winner() { return standings.get(0); }

    /** HH:MM:SS (FR-48). */
    public String formattedDuration() { return GameClock.format(durationSeconds); }
}
