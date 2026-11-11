
package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for Double Block detection and dissolution (FR-30–FR-32). */
class DoubleBlockRulesTest {
    /** Create a deterministic Easy game state with two players. */
    private static GameState newState() {
        return TestSupport.newState(2);
    }

    /** Find a square of the requested type on the Red player's route. */
    private static Position squareOfType(GameState state, CellType type) {
        for (Position square : state.board().route(PlayerColor.RED)) {
            if (state.board().cellAt(square).type() == type) return square;
        }
        throw new AssertionError("Square type not found: " + type);
    }

    /** Place a piece at a physical square on its own movement route. */
    private static void placeAt(GameState state, Piece piece, Position square) {
        int step = state.board().route(piece.color()).indexOf(square);
        assertTrue(step >= 0, "Square is not on the piece's route");
        piece.moveTo(step);
    }

    /** FR-30: an empty square does not contain a Double Block. */
    @Test
    void emptySquareHasNoBlock() {
        GameState state = newState();
        Position square = squareOfType(state, CellType.REGULAR);
        assertFalse(InteractionRules.isDoubleBlock(state, square));
        assertTrue(InteractionRules.blockOwners(state, square).isEmpty());
    }

    /** FR-30: a single piece cannot create a Double Block. */
    @Test
    void onePieceDoesNotFormBlock() {
        GameState state = newState();
        Position square = squareOfType(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        assertFalse(InteractionRules.isDoubleBlock(state, square));
        assertTrue(InteractionRules.blockOwners(state, square).isEmpty());
    }

    /** FR-30: two pieces of the same player form a Double Block. */
    @Test
    void twoFriendlyPiecesFormBlock() {
        GameState state = newState();
        Position square = squareOfType(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        assertTrue(InteractionRules.isDoubleBlock(state, square));
        assertEquals(Set.of(0), InteractionRules.blockOwners(state, square));
    }

    /** FR-30: a block remains valid with three or four pieces. */
    @Test
    void threeAndFourPiecesStillFormBlock() {
        GameState state = newState();
        Position square = squareOfType(state, CellType.REGULAR);
        for (int i = 0; i < 3; i++) placeAt(state, state.piece(i), square);
        assertEquals(Set.of(0), InteractionRules.blockOwners(state, square));
        placeAt(state, state.piece(3), square);
        assertEquals(Set.of(0), InteractionRules.blockOwners(state, square));
        assertTrue(InteractionRules.isDoubleBlock(state, square));
    }

    /** FR-30: pieces on different physical squares do not form a block. */
    @Test
    void piecesOnDifferentSquaresDoNotFormBlock() {
        GameState state = newState();
        Position first = state.board().positionOf(PlayerColor.RED, 1);
        Position second = state.board().positionOf(PlayerColor.RED, 2);
        assertNotEquals(first, second);
        placeAt(state, state.piece(0), first);
        placeAt(state, state.piece(1), second);
        assertFalse(InteractionRules.isDoubleBlock(state, first));
        assertFalse(InteractionRules.isDoubleBlock(state, second));
    }

    /** FR-30: one piece from each player does not form a Double Block. */
    @Test
    void differentOwnersDoNotFormBlockTogether() {
        GameState state = newState();
        Position square = state.board().positionOf(PlayerColor.RED, 0);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(4), square);
        assertEquals(2, InteractionRules.piecesAt(state, square).size());
        assertFalse(InteractionRules.isDoubleBlock(state, square));
        assertTrue(InteractionRules.blockOwners(state, square).isEmpty());
    }

    /** FR-30/FR-31: a block restricts opponents but not its owner. */
    @Test
    void ownBlockDoesNotBlockItsOwner() {
        GameState state = newState();
        Position square = squareOfType(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        assertFalse(InteractionRules.hasOpponentDoubleBlock(state, 0, square));
        assertTrue(InteractionRules.hasOpponentDoubleBlock(state, 1, square));
    }

    /** FR-30: both players can form independent blocks on a protected square. */
    @Test
    void bothPlayersCanOwnBlocksOnProtectedSquare() {
        GameState state = newState();
        Position square = state.board().positionOf(PlayerColor.RED, 0);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        placeAt(state, state.piece(4), square);
        placeAt(state, state.piece(5), square);
        assertEquals(Set.of(0, 1), InteractionRules.blockOwners(state, square));
        assertTrue(InteractionRules.hasOpponentDoubleBlock(state, 0, square));
        assertTrue(InteractionRules.hasOpponentDoubleBlock(state, 1, square));
    }

    /** FR-32: removing one piece from a pair dissolves the Double Block. */
    @Test
    void leavingTwoPieceBlockDissolvesIt() {
        GameState state = newState();
        Position square = squareOfType(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        assertTrue(InteractionRules.isDoubleBlock(state, square));
        state.piece(0).returnToCastle();
        assertFalse(InteractionRules.isDoubleBlock(state, square));
        assertTrue(InteractionRules.blockOwners(state, square).isEmpty());
    }

    /** FR-32: removing one of three pieces leaves a valid block. */
    @Test
    void leavingThreePieceBlockKeepsItActive() {
        GameState state = newState();
        Position square = squareOfType(state, CellType.REGULAR);
        for (int i = 0; i < 3; i++) placeAt(state, state.piece(i), square);
        state.piece(0).returnToCastle();
        assertTrue(InteractionRules.isDoubleBlock(state, square));
        assertEquals(Set.of(0), InteractionRules.blockOwners(state, square));
    }

    /** FR-32: dissolving one player's block does not remove another player's block. */
    @Test
    void removingOneOwnerBlockPreservesOtherOwner() {
        GameState state = newState();
        Position square = state.board().positionOf(PlayerColor.RED, 0);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        placeAt(state, state.piece(4), square);
        placeAt(state, state.piece(5), square);
        state.piece(0).returnToCastle();
        assertEquals(Set.of(1), InteractionRules.blockOwners(state, square));
        assertTrue(InteractionRules.hasOpponentDoubleBlock(state, 0, square));
        assertFalse(InteractionRules.hasOpponentDoubleBlock(state, 1, square));
    }

    /** FR-30: pieces in castles and finish areas cannot form board blocks. */
    @Test
    void castleAndFinishedPiecesDoNotFormBlocks() {
        GameState state = newState();
        assertFalse(InteractionRules.isDoubleBlock(state, null));
        assertTrue(InteractionRules.blockOwners(state, null).isEmpty());
        assertFalse(InteractionRules.hasOpponentDoubleBlock(state, 0, null));
        state.piece(0).moveTo(state.board().finishStep());
        state.piece(1).moveTo(state.board().finishStep());
        assertNull(state.positionOf(state.piece(0)));
        assertNull(state.positionOf(state.piece(1)));
        assertFalse(InteractionRules.isDoubleBlock(state, null));
    }

    /** FR-30: blocks can form on regular and special board squares. */
    @Test
    void blocksWorkOnEveryRelevantCellType() {
        for (CellType type : List.of(CellType.REGULAR, CellType.START,
                CellType.SAFE, CellType.ARROW, CellType.HOME)) {
            GameState state = newState();
            Position square = squareOfType(state, type);
            placeAt(state, state.piece(0), square);
            placeAt(state, state.piece(1), square);
            assertEquals(Set.of(0), InteractionRules.blockOwners(state, square),
                    "Block not detected on " + type);
        }
    }

    /** Verify that callers cannot modify the block-owner set. */
    @Test
    void blockOwnersCollectionIsUnmodifiable() {
        GameState state = newState();
        Position square = squareOfType(state, CellType.REGULAR);
        placeAt(state, state.piece(0), square);
        placeAt(state, state.piece(1), square);
        Set<Integer> owners = InteractionRules.blockOwners(state, square);
        assertThrows(UnsupportedOperationException.class, () -> owners.add(1));
        assertEquals(Set.of(0), InteractionRules.blockOwners(state, square));
    }
}
