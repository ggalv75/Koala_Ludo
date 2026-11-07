package Model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Measures the whole game from the moment the board is shown until it ends (FR-48).
 * The clock is injected so tests can control the time.
 */
public final class GameClock {
    private final Clock clock;
    private final Instant start;
    private Instant end;

    /** Start timing the game using the supplied clock. */
    public GameClock(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.start = clock.instant();
    }

    /** Return elapsed game seconds, frozen after the game ends. */
    public long elapsedSeconds() {
        Instant until = isStopped() ? end : clock.instant();
        return Duration.between(start, until).getSeconds();
    }

    /** Report whether the game timer has stopped. */
    public boolean isStopped() { return end != null; }

    /** Stops the clock once; later calls keep the first end time. */
    void stop() {
        if (!isStopped()) end = clock.instant();
    }

    /** Date and time the game ended, in the clock's time zone. Call after {@link #stop()}. */
    LocalDateTime endedAt() {
        return LocalDateTime.ofInstant(end, clock.getZone());
    }

    /** Formats seconds as HH:MM:SS (FR-48). */
    public static String format(long seconds) {
        long safe = Math.max(0, seconds);
        return String.format("%02d:%02d:%02d", safe / 3600, (safe / 60) % 60, safe % 60);
    }
}
