package Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Game and player setup, FR-01 to FR-06 (SDP 5.2.1: Omer). */
class SetupValidatorTest {

    private static Optional<String> validate(List<String> names, List<PlayerColor> colors) {
        return SetupValidator.validate(Difficulty.EASY, names, colors);
    }

    @Test
    @DisplayName("FR-01: 2, 3 and 4 players are accepted")
    void acceptsTwoToFourPlayers() {
        for (int n = 2; n <= 4; n++) {
            assertTrue(validate(TestSupport.names(n), TestSupport.COLORS.subList(0, n)).isEmpty(), n + " players");
        }
    }

    @Test
    @DisplayName("FR-01: 1 or 5 players are rejected (boundary values)")
    void rejectsWrongPlayerCount() {
        assertTrue(validate(List.of("A"), List.of(PlayerColor.RED)).isPresent());
        List<PlayerColor> five = new ArrayList<>(TestSupport.COLORS);
        five.add(PlayerColor.RED);
        assertTrue(validate(TestSupport.names(5), five).isPresent());
    }

    @Test
    @DisplayName("FR-05: empty or blank name is rejected")
    void rejectsBlankName() {
        assertTrue(validate(List.of("Gal", "   "), List.of(PlayerColor.RED, PlayerColor.BLUE)).isPresent());
    }

    @Test
    @DisplayName("FR-05: a missing colour is rejected")
    void rejectsMissingColor() {
        assertTrue(validate(List.of("Gal", "Dor"), Arrays.asList(PlayerColor.RED, null)).isPresent());
    }

    @Test
    @DisplayName("FR-03: the same colour cannot be given to two players")
    void rejectsDuplicateColor() {
        Optional<String> problem = validate(List.of("Gal", "Dor"), List.of(PlayerColor.RED, PlayerColor.RED));
        assertTrue(problem.isPresent());
        assertTrue(problem.get().contains(PlayerColor.RED.displayName()));
    }

    @Test
    @DisplayName("FR-05: names must differ (ignoring case and spaces)")
    void rejectsDuplicateName() {
        assertTrue(validate(List.of("Gal", " gal "), List.of(PlayerColor.RED, PlayerColor.BLUE)).isPresent());
    }

    @Test
    @DisplayName("Names longer than 15 characters are rejected, 15 is accepted (boundary values)")
    void nameLengthBoundary() {
        List<PlayerColor> colors = List.of(PlayerColor.RED, PlayerColor.BLUE);
        assertTrue(validate(List.of("A".repeat(15), "B"), colors).isEmpty());
        assertTrue(validate(List.of("A".repeat(16), "B"), colors).isPresent());
    }

    @Test
    @DisplayName("FR-05: the engine refuses invalid settings")
    void engineRejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class, () -> new GameEngine(Difficulty.EASY,
                List.of("Gal", "Dor"), List.of(PlayerColor.RED, PlayerColor.RED)));
    }

    @Test
    @DisplayName("FR-04 / FR-06: four pieces per player in the castle, level fixed to the chosen one")
    void initialState() {
        GameEngine engine = new GameEngine(Difficulty.EASY, List.of(" Gal ", "Dor", "Omer"),
                List.of(PlayerColor.BLUE, PlayerColor.RED, PlayerColor.GREEN));
        GameState state = engine.state();
        assertEquals(12, state.pieces().size());
        for (Player player : state.players()) {
            List<Piece> own = state.piecesOf(player);
            assertEquals(4, own.size());
            own.forEach(p -> {
                assertTrue(p.isInCastle());
                assertEquals(player.color(), p.color());
            });
        }
        assertEquals("Gal", state.players().get(0).name(), "Names are trimmed");
        assertEquals(Difficulty.EASY, state.difficulty());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
    }
}
