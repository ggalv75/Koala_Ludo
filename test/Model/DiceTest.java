package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-15: the regular step die. Owner: Gal (SDP 5.2.1). */
class DiceTest {

    @Test
    @DisplayName("FR-15: every roll is between 1 and 6 and all six faces appear")
    void rollIsAlwaysOneToSixAndEveryFaceAppears() {
        Dice dice = new Dice(new Random(7));
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            int value = dice.roll().value();
            assertTrue(value >= 1 && value <= 6, "Roll out of range: " + value);
            seen.add(value);
        }
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), seen);
    }

    @Test
    @DisplayName("FR-15 boundary values: 1 and 6 are valid die values")
    void lowestAndHighestValuesAreAccepted() {
        assertEquals(1, new DiceRoll(1).value());
        assertEquals(6, new DiceRoll(6).value());
    }

    @Test
    @DisplayName("FR-15 boundary values: 0 and 7 are rejected")
    void valuesOutsideTheDieAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(0));
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(7));
    }

    @Test
    @DisplayName("FR-19: only 6 counts as a six")
    void onlySixIsSix() {
        assertTrue(new DiceRoll(6).isSix());
        assertFalse(new DiceRoll(5).isSix());
    }
}
