
package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for player, piece and initial game state (FR-01–FR-06). */
class GameStateInitializationTest {
    private static final List<String> NAMES = List.of("Dana", "Omer", "Gal", "Noa");
    private static final List<PlayerColor> COLORS = List.of(
            PlayerColor.BLUE, PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW);

    /** Create a deterministic Easy game with the requested player count. */
    private static GameState newState(int playerCount) {
        return new GameEngine(Difficulty.EASY,
                NAMES.subList(0, playerCount),
                COLORS.subList(0, playerCount),
                new Random(42), new Dice(new Random(7)), Clock.systemUTC()).state();
    }

    /** FR-01/FR-03: verify player initialization for 2–4 players. */
    @Test
    void initializesCorrectNumberOfPlayers() {
        for (int count = 2; count <= 4; count++) {
            GameState state = newState(count);
            assertEquals(count, state.players().size());
            for (int i = 0; i < count; i++) {
                assertEquals(i, state.players().get(i).index());
                assertEquals(NAMES.get(i), state.players().get(i).name());
            }
        }
    }

    /** FR-03: verify that every player receives the selected color. */
    @Test
    void assignsCorrectPlayerColors() {
        for (int count = 2; count <= 4; count++) {
            GameState state = newState(count);
            Set<PlayerColor> assigned = new HashSet<>();
            for (int i = 0; i < count; i++) {
                Player player = state.players().get(i);
                assertEquals(COLORS.get(i), player.color());
                assertTrue(assigned.add(player.color()), "Duplicate player color");
            }
        }
    }

    /** FR-04: verify exactly four pieces per player. */
    @Test
    void createsFourPiecesPerPlayer() {
        for (int count = 2; count <= 4; count++) {
            GameState state = newState(count);
            assertEquals(count * GameRules.PIECES_PER_PLAYER, state.pieces().size());
            for (Player player : state.players()) {
                assertEquals(4, state.piecesOf(player).size());
            }
        }
    }

    /** FR-04: verify that every piece belongs to the correct player. */
    @Test
    void assignsCorrectPieceOwnership() {
        GameState state = newState(4);
        for (Player player : state.players()) {
            for (Piece piece : state.piecesOf(player)) {
                assertEquals(player.index(), piece.ownerIndex());
            }
        }
    }

    /** FR-03/FR-04: verify that all pieces match their owner's color. */
    @Test
    void assignsCorrectPieceColors() {
        for (int count = 2; count <= 4; count++) {
            GameState state = newState(count);
            for (Player player : state.players()) {
                for (Piece piece : state.piecesOf(player)) {
                    assertEquals(player.color(), piece.color());
                }
            }
        }
    }

    /** FR-04: verify that all pieces start inside their owner's castle. */
    @Test
    void allPiecesStartInCastle() {
        for (int count = 2; count <= 4; count++) {
            GameState state = newState(count);
            for (Piece piece : state.pieces()) {
                assertTrue(piece.isInCastle());
                assertEquals(Piece.IN_CASTLE, piece.step());
                assertNull(state.positionOf(piece));
                assertFalse(state.isFinished(piece));
            }
        }
    }

    /** FR-04: verify unique piece IDs and numbering from 1 to 4. */
    @Test
    void pieceIdentifiersAndNumbersAreCorrect() {
        GameState state = newState(4);
        Set<Integer> ids = new HashSet<>();
        for (Piece piece : state.pieces()) {
            assertTrue(ids.add(piece.id()), "Duplicate piece ID");
            assertSame(piece, state.piece(piece.id()));
        }
        assertEquals(16, ids.size());
        for (Player player : state.players()) {
            Set<Integer> numbers = new HashSet<>();
            for (Piece piece : state.piecesOf(player)) {
                numbers.add(piece.number());
            }
            assertEquals(Set.of(1, 2, 3, 4), numbers);
        }
    }

    /** FR-05: verify that the first player begins in the waiting-for-roll phase. */
    @Test
    void firstPlayerStartsTheGame() {
        GameState state = newState(4);
        assertEquals(0, state.currentPlayerIndex());
        assertSame(state.players().get(0), state.currentPlayer());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
    }

    /** Verify that no dice result or legal move exists before the first roll. */
    @Test
    void initialTurnHasNoRollOrLegalMoves() {
        GameState state = newState(4);
        assertNull(state.lastRoll());
        assertNull(state.latestRoll());
        assertEquals(0, state.consecutiveSixes());
        assertTrue(state.legalMoves().isEmpty());
        for (Piece piece : state.pieces()) {
            assertFalse(state.canMove(piece));
        }
    }

    /** FR-04/FR-06: verify the initial level, progress and result. */
    @Test
    void initializesGameProgressCorrectly() {
        GameState state = newState(4);
        assertEquals(Difficulty.EASY, state.difficulty());
        assertNull(state.result());
        assertNull(state.feedbackMessage());
        assertEquals(List.of("המשחק התחיל!"), state.recentLog());
        for (Player player : state.players()) {
            assertEquals(0, state.finishedPieceCount(player));
            assertEquals(0, player.rank());
            assertFalse(player.isFinished());
        }
    }

    /** Verify that the public player and piece lists cannot be modified. */
    @Test
    void publicCollectionsAreUnmodifiable() {
        GameState state = newState(2);
        assertThrows(UnsupportedOperationException.class, () -> state.players().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.pieces().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.recentLog().clear());
    }

    /** Verify that different games do not share mutable piece state. */
    @Test
    void separateGamesHaveIndependentPieces() {
        GameState first = newState(2);
        GameState second = newState(2);
        assertNotSame(first, second);
        assertNotSame(first.piece(0), second.piece(0));
        first.piece(0).moveTo(0);
        assertEquals(0, first.piece(0).step());
        assertTrue(second.piece(0).isInCastle());
        assertEquals(Piece.IN_CASTLE, second.piece(0).step());
    }
}
