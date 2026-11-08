package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-48: game duration and its HH:MM:SS format. Owner: Gal (SDP 5.2.1). */
class GameClockTest {

    @Test
    @DisplayName("FR-48: the duration is shown as HH:MM:SS")
    void formatsAsHoursMinutesSeconds() {
        assertEquals("00:00:00", GameClock.format(0));
        assertEquals("00:00:59", GameClock.format(59));
        assertEquals("00:01:00", GameClock.format(60));
        assertEquals("01:01:01", GameClock.format(3661));
    }

    @Test
    @DisplayName("FR-48 boundary: a negative value is shown as zero")
    void negativeDurationIsShownAsZero() {
        assertEquals("00:00:00", GameClock.format(-5));
    }

    @Test
    @DisplayName("FR-48: the clock counts the time since the game started")
    void countsElapsedTime() {
        TestSupport.ManualClock time = new TestSupport.ManualClock();
        GameClock clock = new GameClock(time);
        time.advance(Duration.ofSeconds(90));
        assertEquals(90, clock.elapsedSeconds());
        assertFalse(clock.isStopped());
    }

    @Test
    @DisplayName("FR-48: after the game ends the clock no longer advances")
    void stoppedClockKeepsItsTime() {
        TestSupport.ManualClock time = new TestSupport.ManualClock();
        GameClock clock = new GameClock(time);
        time.advance(Duration.ofSeconds(100));
        clock.stop();
        time.advance(Duration.ofSeconds(500));

        assertTrue(clock.isStopped());
        assertEquals(100, clock.elapsedSeconds());
    }

    @Test
    @DisplayName("FR-48: stopping twice keeps the first end time")
    void secondStopDoesNotChangeEndTime() {
        TestSupport.ManualClock time = new TestSupport.ManualClock();
        GameClock clock = new GameClock(time);
        time.advance(Duration.ofSeconds(10));
        clock.stop();
        time.advance(Duration.ofSeconds(10));
        clock.stop();

        assertEquals(10, clock.elapsedSeconds());
    }
}
