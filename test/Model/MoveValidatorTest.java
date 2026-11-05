package Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Piece movement and blocks, FR-23 to FR-27, FR-30, FR-31 (SDP 5.2.1: Diana). */
class MoveValidatorTest {
    private GameState state;
    private Board board;

    @BeforeEach
    void setUp() {
        state = TestSupport.state(3, 11);
        board = state.board();
    }

    private Piece red(int number) { return TestSupport.piece(state, 0, number); }
    private Piece green(int number) { return TestSupport.piece(state, 1, number); }
    private Piece yellow(int number) { return TestSupport.piece(state, 2, number); }

    private int greenStepAtRed(int redStep) {
        return TestSupport.sameSquare(board, PlayerColor.GREEN, PlayerColor.RED, redStep);
    }

    private List<Move> moves(int die) { return MoveValidator.legalMoves(state, new DiceRoll(die)); }

    @Test
    @DisplayName("FR-24: a piece leaves the castle only with a six")
    void releaseNeedsSix() {
        for (int die = 1; die <= 5; die++) assertTrue(moves(die).isEmpty(), "die " + die);
        List<Move> six = moves(6);
        assertEquals(4, six.size());
        six.forEach(m -> {
            assertTrue(m.release());
            assertEquals(0, m.toStep());
        });
    }

    @Test
    @DisplayName("FR-30: an opponent Double Block on my start square keeps my castle closed")
    void releaseBlockedByOpponentBlock() {
        green(1).moveTo(greenStepAtRed(0));
        green(2).moveTo(greenStepAtRed(0));
        assertTrue(moves(6).isEmpty());
    }

    @Test
    @DisplayName("FR-24: release onto a single opponent piece is allowed and captures nothing")
    void releaseOntoSingleOpponent() {
        green(1).moveTo(greenStepAtRed(0));
        assertEquals(4, moves(6).size());
        red(1).moveTo(0);
        assertTrue(InteractionRules.capturableOpponent(state, red(1)).isEmpty());
    }

    @Test
    @DisplayName("FR-24: release is allowed when own pieces are already on the start square")
    void releaseOntoOwnPieces() {
        red(1).moveTo(0);
        red(2).moveTo(0);
        long releases = moves(6).stream().filter(Move::release).count();
        assertEquals(2, releases);
    }

    @Test
    @DisplayName("FR-26: the finish needs an exact roll; a smaller roll moves inside the home path")
    void exactFinish() {
        red(1).moveTo(board.finishStep() - 3);
        assertEquals(board.finishStep(), moves(3).get(0).toStep());
        assertTrue(moves(4).isEmpty(), "Overshoot is not a legal move");
        assertEquals(board.finishStep() - 1, moves(2).get(0).toStep());
    }

    @Test
    @DisplayName("FR-25: a piece passes its arrow and continues into its own home path")
    void arrowLeadsHome() {
        int arrow = board.homePathStart() - 1;
        red(1).moveTo(arrow - 2);
        Move move = moves(4).get(0);
        assertEquals(arrow + 2, move.toStep());
        assertEquals(CellType.HOME, board.cellAt(PlayerColor.RED, move.toStep()).type());
    }

    @Test
    @DisplayName("FR-31: if one piece is stopped by a block, a piece that can use the whole roll must move")
    void fullMoveHasPriority() {
        red(1).moveTo(20);
        red(2).moveTo(5);
        green(1).moveTo(greenStepAtRed(23));
        green(2).moveTo(greenStepAtRed(23));
        List<Move> legal = moves(4);
        assertEquals(1, legal.size());
        assertEquals(red(2).id(), legal.get(0).pieceId());
        assertFalse(legal.get(0).partial());
    }

    @Test
    @DisplayName("FR-31: with no full move, the piece stops on the square before the block")
    void partialMoveStopsBeforeBlock() {
        red(1).moveTo(20);
        green(1).moveTo(greenStepAtRed(23));
        green(2).moveTo(greenStepAtRed(23));
        List<Move> legal = moves(5);
        assertEquals(1, legal.size());
        assertTrue(legal.get(0).partial());
        assertEquals(22, legal.get(0).toStep());
    }

    @Test
    @DisplayName("FR-30: a piece cannot land on an opponent's Double Block")
    void cannotLandOnBlock() {
        red(1).moveTo(20);
        green(1).moveTo(greenStepAtRed(23));
        green(2).moveTo(greenStepAtRed(23));
        assertEquals(22, moves(3).get(0).toStep());
    }

    @Test
    @DisplayName("FR-31: a block on the very next square leaves no move")
    void blockRightAhead() {
        red(1).moveTo(20);
        green(1).moveTo(greenStepAtRed(21));
        green(2).moveTo(greenStepAtRed(21));
        assertTrue(moves(4).isEmpty());
    }

    @Test
    @DisplayName("FR-30: a player's own Double Block does not stop the player")
    void ownBlockIsPassable() {
        red(1).moveTo(20);
        red(2).moveTo(23);
        red(3).moveTo(23);
        assertTrue(moves(5).stream().anyMatch(m -> m.pieceId() == red(1).id() && m.toStep() == 25 && !m.partial()));
    }

    @Test
    @DisplayName("FR-30: one piece each of two different opponents is not a block")
    void differentOpponentsAreNotABlock() {
        red(1).moveTo(20);
        green(1).moveTo(greenStepAtRed(23));
        yellow(1).moveTo(TestSupport.sameSquare(board, PlayerColor.YELLOW, PlayerColor.RED, 23));
        assertEquals(25, moves(5).get(0).toStep());
    }

    @Test
    @DisplayName("Finished pieces never move again")
    void finishedPiecesDoNotMove() {
        red(1).moveTo(board.finishStep());
        assertTrue(moves(3).isEmpty());
    }
}
