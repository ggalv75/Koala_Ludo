package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * FR-15, FR-16: the Boost and UltraBoost dice and the multiplier coefficient N - 1.
 * Black-box: equivalence classes and boundary values for each die. Owner: Gal (iteration 2).
 */
class MultiplierDiceTest {

    @Test
    @DisplayName("FR-15: Boost rolls are 1-3 and every face appears")
    void boostRollsOneToThree() {
        assertEquals(Set.of(1, 2, 3), facesSeen(MultiplierDie.BOOST, 3_000));
    }

    @Test
    @DisplayName("FR-15: UltraBoost rolls are 1-6 and every face appears")
    void ultraBoostRollsOneToSix() {
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), facesSeen(MultiplierDie.ULTRA_BOOST, 6_000));
    }

    @Test
    @DisplayName("FR-15: each Boost face comes up with roughly equal probability")
    void boostFacesHaveEqualProbability() {
        Dice dice = new Dice(new Random(2026));
        int rolls = 30_000;
        int[] counts = new int[4];
        for (int i = 0; i < rolls; i++) counts[dice.rollMultiplier(MultiplierDie.BOOST)]++;
        int expected = rolls / 3;
        for (int face = 1; face <= 3; face++) {
            assertTrue(Math.abs(counts[face] - expected) < expected * 0.05,
                    "Boost face " + face + " appeared " + counts[face] + " times");
        }
    }

    @Test
    @DisplayName("FR-15: there is nothing to roll for NONE")
    void noneCannotBeRolled() {
        assertThrows(IllegalArgumentException.class, () -> new Dice().rollMultiplier(MultiplierDie.NONE));
    }

    @Test
    @DisplayName("FR-15 boundary values: Boost accepts 1 and 3, rejects 0 and 4")
    void boostBoundaries() {
        assertEquals(1, new DiceRoll(4, MultiplierDie.BOOST, 1).multiplierValue());
        assertEquals(3, new DiceRoll(4, MultiplierDie.BOOST, 3).multiplierValue());
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(4, MultiplierDie.BOOST, 0));
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(4, MultiplierDie.BOOST, 4));
    }

    @Test
    @DisplayName("FR-15 boundary values: UltraBoost accepts 1 and 6, rejects 0 and 7")
    void ultraBoostBoundaries() {
        assertEquals(1, new DiceRoll(4, MultiplierDie.ULTRA_BOOST, 1).multiplierValue());
        assertEquals(6, new DiceRoll(4, MultiplierDie.ULTRA_BOOST, 6).multiplierValue());
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(4, MultiplierDie.ULTRA_BOOST, 0));
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(4, MultiplierDie.ULTRA_BOOST, 7));
    }

    @Test
    @DisplayName("FR-15: a roll without a multiplier die cannot carry a multiplier value")
    void noneMustHaveZeroValue() {
        assertThrows(IllegalArgumentException.class, () -> new DiceRoll(4, MultiplierDie.NONE, 2));
        assertThrows(NullPointerException.class, () -> new DiceRoll(4, null, 0));
    }

    @Test
    @DisplayName("FR-16: steps on the track = die x (N - 1), including 0")
    void trackStepsUseTheCoefficient() {
        // {die, multiplier value, expected coefficient, expected steps}
        int[][] boost = {{4, 1, 0, 0}, {4, 2, 1, 4}, {4, 3, 2, 8}, {1, 3, 2, 2}, {6, 3, 2, 12}};
        int[][] ultra = {{4, 1, 0, 0}, {4, 6, 5, 20}, {6, 6, 5, 30}, {1, 2, 1, 1}};
        assertAll(rows(MultiplierDie.BOOST, boost));
        assertAll(rows(MultiplierDie.ULTRA_BOOST, ultra));
    }

    @Test
    @DisplayName("FR-16: without a multiplier die the steps are the die value")
    void noMultiplierMeansDieValue() {
        DiceRoll roll = new DiceRoll(5);
        assertFalse(roll.hasMultiplier());
        assertEquals(1, roll.factor());
        assertEquals(5, roll.trackSteps());
    }

    @Test
    @DisplayName("FR-17: on the home path only the step die counts")
    void homePathIgnoresMultiplier() {
        assertEquals(4, new DiceRoll(4, MultiplierDie.ULTRA_BOOST, 6).homePathSteps());
        assertEquals(4, new DiceRoll(4, MultiplierDie.BOOST, 1).homePathSteps());
    }

    @Test
    @DisplayName("FR-19, FR-24: only the step die decides a six")
    void sixIsDecidedByTheStepDie() {
        assertTrue(new DiceRoll(6, MultiplierDie.BOOST, 1).isSix());
        assertFalse(new DiceRoll(5, MultiplierDie.ULTRA_BOOST, 6).isSix());
    }

    @Test
    @DisplayName("NFR-05: the multiplier die is defined with the level; Easy has none")
    void easyHasNoMultiplierDie() {
        assertEquals(MultiplierDie.NONE, Difficulty.EASY.multiplierDie());
    }

    private static Set<Integer> facesSeen(MultiplierDie die, int rolls) {
        Dice dice = new Dice(new Random(7));
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < rolls; i++) {
            int value = dice.rollMultiplier(die);
            assertTrue(die.accepts(value), die + " out of range: " + value);
            seen.add(value);
        }
        return seen;
    }

    private static Executable[] rows(MultiplierDie die, int[][] table) {
        Executable[] checks = new Executable[table.length];
        for (int i = 0; i < table.length; i++) {
            int[] row = table[i];
            checks[i] = () -> {
                DiceRoll roll = new DiceRoll(row[0], die, row[1]);
                String label = die + " die=" + row[0] + " N=" + row[1];
                assertEquals(row[2], roll.factor(), label + " coefficient");
                assertEquals(row[3], roll.trackSteps(), label + " steps");
            };
        }
        return checks;
    }
}
