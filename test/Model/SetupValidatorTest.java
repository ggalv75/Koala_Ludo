
package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for game setup validation (FR-01, FR-03, FR-05). */
class SetupValidatorTest {
    /** Validate settings for the Easy difficulty. */
    private static boolean valid(List<String> names, List<PlayerColor> colors) {
        return SetupValidator.validate(Difficulty.EASY, names, colors).isEmpty();
    }

    /** FR-01: allow a game with two players. */
    @Test
    void acceptsTwoPlayers() {
        assertTrue(valid(List.of("Noa", "Dan"), List.of(PlayerColor.RED, PlayerColor.BLUE)));
    }

    /** FR-01: allow a game with three players. */
    @Test
    void acceptsThreePlayers() {
        assertTrue(valid(List.of("Noa", "Dan", "Adi"),
                List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN)));
    }

    /** FR-01: allow a game with four players. */
    @Test
    void acceptsFourPlayers() {
        assertTrue(valid(List.of("Noa", "Dan", "Adi", "Gal"),
                List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN, PlayerColor.YELLOW)));
    }

    /** FR-01: reject a game with fewer than two players. */
    @Test
    void rejectsOnePlayer() {
        assertFalse(valid(List.of("Noa"), List.of(PlayerColor.RED)));
    }

    /** FR-01: reject a game with more than four players. */
    @Test
    void rejectsFivePlayers() {
        assertFalse(valid(List.of("Noa", "Dan", "Adi", "Gal", "Ron"),
                List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN,
                        PlayerColor.YELLOW, PlayerColor.RED)));
    }

    /** FR-05: reject a missing difficulty. */
    @Test
    void rejectsNullDifficulty() {
        assertTrue(SetupValidator.validate(null, List.of("Noa", "Dan"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)).isPresent());
    }

    /** FR-05: reject missing player names or color lists. */
    @Test
    void rejectsNullLists() {
        assertTrue(SetupValidator.validate(Difficulty.EASY, null,
                List.of(PlayerColor.RED, PlayerColor.BLUE)).isPresent());
        assertTrue(SetupValidator.validate(Difficulty.EASY,
                List.of("Noa", "Dan"), null).isPresent());
    }

    /** FR-05: reject mismatched numbers of names and colors. */
    @Test
    void rejectsMismatchedListSizes() {
        assertFalse(valid(List.of("Noa", "Dan"),
                List.of(PlayerColor.RED)));
    }

    /** FR-03: reject null, empty and whitespace-only names. */
    @Test
    void rejectsMissingPlayerNames() {
        assertFalse(valid(Arrays.asList(null, "Dan"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
        assertFalse(valid(List.of("", "Dan"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
        assertFalse(valid(List.of("   ", "Dan"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
    }

    /** FR-03: accept a name containing exactly 15 characters. */
    @Test
    void acceptsMaximumNameLength() {
        assertEquals(15, "ABCDEFGHIJKLMNO".length());
        assertTrue(valid(List.of("ABCDEFGHIJKLMNO", "Dan"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
    }

    /** Verify that names longer than the configured limit are rejected. */
    @Test
    void rejectsNameLongerThanMaximum() {
        assertFalse(valid(List.of("ABCDEFGHIJKLMNOP", "Dan"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
    }

    /** Verify that surrounding whitespace is removed before checking length. */
    @Test
    void trimsWhitespaceBeforeLengthValidation() {
        assertTrue(valid(List.of("  ABCDEFGHIJKLMNO  ", "Dan"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
    }

    /** FR-03: reject identical player names. */
    @Test
    void rejectsDuplicateNames() {
        assertFalse(valid(List.of("Noa", "Noa"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
    }

    /** FR-03: reject names differing only by letter case. */
    @Test
    void rejectsCaseInsensitiveDuplicateNames() {
        assertFalse(valid(List.of("Player1", "player1"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
    }

    /** FR-03: reject duplicate names with surrounding whitespace. */
    @Test
    void rejectsDuplicateNamesAfterTrimming() {
        assertFalse(valid(List.of("Noa", "  Noa  "),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
    }

    /** FR-03: reject duplicate player colors. */
    @Test
    void rejectsDuplicateColors() {
        assertFalse(valid(List.of("Noa", "Dan"),
                List.of(PlayerColor.RED, PlayerColor.RED)));
    }

    /** FR-05: reject a player without a selected color. */
    @Test
    void rejectsMissingColor() {
        assertFalse(valid(List.of("Noa", "Dan"),
                Arrays.asList(PlayerColor.RED, null)));
    }

    /** FR-03: allow nicknames containing numbers and special characters. */
    @Test
    void acceptsSpecialCharactersInNames() {
        assertTrue(valid(List.of("@113%", "Player_2"),
                List.of(PlayerColor.RED, PlayerColor.BLUE)));
        assertTrue(valid(List.of("Noa, Queen", "The \"King\""),
                List.of(PlayerColor.GREEN, PlayerColor.YELLOW)));
    }
}
