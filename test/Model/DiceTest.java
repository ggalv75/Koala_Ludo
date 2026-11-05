package Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Regular die, FR-15 (SDP 5.2.1: Gal). */
class DiceTest {

    @Test
    @DisplayName("FR-15: every roll is 1-6 and all faces appear about equally often")
    void rollsAreFair() {
        Dice dice = new Dice(new Random(1));
        int[] counts = new int[7];
        for (int i = 0; i < 6000; i++) {
            int value = dice.roll().value();
            assertTrue(value >= 1 && value <= 6, "value " + value);
            counts[value]++;
        }
        for (int face = 1; face <= 6; face++) {
            assertTrue(counts[face] > 850 && counts[face] < 1150, "face " + face + " appeared " + counts[face]);
        }
    }

    @Test
    @DisplayName("A die value outside 1-6 cannot exist (boundary values 0 and 7)")
    void invalidValuesRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(0));
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(7));
        assertEquals(1, new DiceRoll(1).value());
        assertEquals(6, new DiceRoll(6).value());
    }

    @Test
    @DisplayName("Only 6 counts as a six")
    void sixDetection() {
        assertTrue(new DiceRoll(6).isSix());
        assertFalse(new DiceRoll(5).isSix());
    }
}
