
package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for capturing and protected squares (FR-28, FR-29). */
class InteractionRulesTest {
    /** Create a deterministic game state with Red and Green players. */
    private static GameState newState() {
        return TestSupport.newState(2);
    }

    /** Find a square shared by both players with the requested cell type. */
    private static Position sharedSquare(GameState state, CellType type) {
        List<Position> greenRoute = state.board().route(PlayerColor.GREEN);
        for (Position square : state.board().route(PlayerColor.RED)) {
            Cell cell = state.board().cellAt(square);
            if (cell != null && cell.type() == type && greenRoute.contains(square)) {
                return square;
            }
        }
        throw new AssertionError("No shared square of type " + type);
    }

    /** Position a piece on a physical square of its own route. */
    private static void placeAt(GameState state, Piece piece, Position square) {
        int step = state.board().route(piece.color()).indexOf(square);
        assertTrue(step >= 0, "Square is not on the player's route");
        piece.moveTo(step);
    }

    /** Verify that pieces in castles and finish areas are not board occupants. */
    @Test
    void castleAndFinishedPiecesAreNotOnBoard() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.REGULAR);
        assertTrue(InteractionRules.piecesAt(state, square).isEmpty());
        state.piece(0).moveTo(state.board().finishStep());
        assertTrue(InteractionRules.piecesAt(state, square).isEmpty());
        assertTrue(InteractionRules.piecesAt(state, null).isEmpty());
    }

    /** Verify that piecesAt returns all pieces sharing a physical square. */
    @Test
    void findsPiecesFromDifferentPlayers() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.START);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(4), square);
        List<Piece> occupants = InteractionRules.piecesAt(state, square);
        assertEquals(2, occupants.size());
        assertTrue(occupants.contains(state.piece(0)));
        assertTrue(occupants.contains(state.piece(4)));
    }

    /** Verify that the returned occupant list cannot be modified. */
    @Test
    void occupantListIsUnmodifiable() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        List<Piece> occupants = InteractionRules.piecesAt(state, square);
        assertThrows(UnsupportedOperationException.class, occupants::clear);
    }

    /** FR-28: a single opponent on a regular square can be captured. */
    @Test
    void capturesSingleOpponentOnRegularSquare() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(4), square);
        Optional<Piece> victim = InteractionRules.capturableOpponent(state, state.piece(0));
        assertTrue(victim.isPresent());
        assertSame(state.piece(4), victim.get());
    }

    /** FR-28: an arrow square is not protected from capture. */
    @Test
    void capturesOpponentOnArrowSquare() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.ARROW);
        assertFalse(state.board().cellAt(square).isProtected());
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(4), square);
        assertEquals(Optional.of(state.piece(4)),
                InteractionRules.capturableOpponent(state, state.piece(0)));
    }

    /** FR-28: pieces belonging to the same player are not captured. */
    @Test
    void doesNotCaptureOwnPieces() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        assertTrue(InteractionRules.capturableOpponent(state, state.piece(0)).isEmpty());
    }

    /** FR-28: a friendly piece does not prevent capturing one opponent. */
    @Test
    void capturesOpponentWhenOwnPieceIsAlsoPresent() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        placeAt(state, state.piece(4), square);
        assertEquals(Optional.of(state.piece(4)),
                InteractionRules.capturableOpponent(state, state.piece(0)));
    }

    /** FR-30: two opposing pieces are not a single capturable target. */
    @Test
    void doesNotCaptureTwoOpponents() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(4), square);
        placeAt(state, state.piece(5), square);
        assertTrue(InteractionRules.capturableOpponent(state, state.piece(0)).isEmpty());
    }

    /** FR-29: protected START and SAFE squares prevent capture. */
    @Test
    void protectedStartAndSafeSquaresPreventCapture() {
        for (CellType type : List.of(CellType.START, CellType.SAFE)) {
            GameState state = newState();
            Position square = sharedSquare(state, type);
            assertTrue(state.board().cellAt(square).isProtected());
            placeAt(state, state.piece(0), square);
            placeAt(state, state.piece(4), square);
            assertTrue(InteractionRules.capturableOpponent(state, state.piece(0)).isEmpty());
            assertFalse(state.piece(4).isInCastle());
        }
    }

    /** FR-29: every square in a player's home lane is protected. */
    @Test
    void homeLaneSquaresAreProtected() {
        GameState state = newState();
        for (PlayerColor color : PlayerColor.values()) {
            for (int step = state.board().homePathStart(); step < state.board().finishStep(); step++) {
                Cell cell = state.board().cellAt(color, step);
                assertNotNull(cell);
                assertEquals(CellType.HOME, cell.type());
                assertTrue(cell.isProtected());
            }
        }
    }

    /** FR-29: shared protected squares are detected when an opponent is present. */
    @Test
    void detectsProtectedLandingWithOpponent() {
        for (CellType type : List.of(CellType.START, CellType.SAFE)) {
            GameState state = newState();
            Position square = sharedSquare(state, type);
            placeAt(state, state.piece(0), square);
            placeAt(state, state.piece(4), square);
            assertTrue(InteractionRules.sharesProtectedSquareWithOpponent(
                    state, state.piece(0)));
        }
    }

    /** FR-29: own pieces alone do not trigger opponent protection. */
    @Test
    void ownPiecesDoNotTriggerProtectedLanding() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.START);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        assertFalse(InteractionRules.sharesProtectedSquareWithOpponent(
                state, state.piece(0)));
    }

    /** FR-29: regular squares do not trigger protected landing events. */
    @Test
    void regularSquareDoesNotTriggerProtection() {
        GameState state = newState();
        Position square = sharedSquare(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(4), square);
        assertFalse(InteractionRules.sharesProtectedSquareWithOpponent(
                state, state.piece(0)));
    }
}
