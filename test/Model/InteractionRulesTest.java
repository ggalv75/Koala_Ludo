package Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Capture, protection and Double Block, FR-28 to FR-30, FR-32 (SDP 5.2.1: Omer). */
class InteractionRulesTest {

    @Test
    @DisplayName("FR-28: landing on a single opponent piece on a regular square sends it to its castle")
    void captureOnRegularSquare() {
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(4));
        GameState state = engine.state();
        Board board = state.board();
        int target = TestSupport.regularStepAfter(board, PlayerColor.RED, 20);
        Piece red = TestSupport.piece(state, 0, 1);
        Piece green = TestSupport.piece(state, 1, 1);
        red.moveTo(target - 4);
        green.moveTo(TestSupport.sameSquare(board, PlayerColor.GREEN, PlayerColor.RED, target));

        engine.rollDice();
        assertTrue(engine.movePiece(red.id()));

        assertEquals(target, red.step());
        assertTrue(green.isInCastle());
    }

    @Test
    @DisplayName("FR-29: no capture on a safe square")
    void noCaptureOnSafeSquare() {
        GameState state = TestSupport.state(2, 5);
        Board board = state.board();
        int safe = -1;
        for (int step = 1; step < board.homePathStart(); step++) {
            if (board.cellAt(PlayerColor.RED, step).type() == CellType.SAFE
                    && board.route(PlayerColor.GREEN).contains(board.positionOf(PlayerColor.RED, step))) {
                safe = step;
                break;
            }
        }
        assertTrue(safe > 0);
        Piece red = TestSupport.piece(state, 0, 1);
        Piece green = TestSupport.piece(state, 1, 1);
        green.moveTo(TestSupport.sameSquare(board, PlayerColor.GREEN, PlayerColor.RED, safe));
        red.moveTo(safe);
        assertTrue(InteractionRules.capturableOpponent(state, red).isEmpty());
    }

    @Test
    @DisplayName("FR-08 / FR-29: no capture on another player's start square")
    void noCaptureOnStartSquare() {
        GameState state = TestSupport.state(2, 5);
        Piece green = TestSupport.piece(state, 1, 1);
        Piece red = TestSupport.piece(state, 0, 1);
        green.moveTo(0);
        red.moveTo(13); // the same square: green's start
        assertEquals(state.positionOf(green), state.positionOf(red));
        assertTrue(InteractionRules.capturableOpponent(state, red).isEmpty());
    }

    @Test
    @DisplayName("FR-30: two pieces of one player form a block for opponents only; it is not captured")
    void doubleBlock() {
        GameState state = TestSupport.state(2, 5);
        Board board = state.board();
        int square = TestSupport.regularStepAfter(board, PlayerColor.RED, 15);
        Position p = board.positionOf(PlayerColor.RED, square);
        int greenStep = TestSupport.sameSquare(board, PlayerColor.GREEN, PlayerColor.RED, square);
        TestSupport.piece(state, 1, 1).moveTo(greenStep);
        TestSupport.piece(state, 1, 2).moveTo(greenStep);

        assertTrue(InteractionRules.isDoubleBlock(state, p));
        assertTrue(InteractionRules.hasOpponentDoubleBlock(state, 0, p));
        assertFalse(InteractionRules.hasOpponentDoubleBlock(state, 1, p));
        Piece red = TestSupport.piece(state, 0, 1);
        red.moveTo(square);
        assertTrue(InteractionRules.capturableOpponent(state, red).isEmpty());
    }

    @Test
    @DisplayName("FR-32: the block disappears as soon as fewer than two pieces remain")
    void blockDissolves() {
        GameState state = TestSupport.state(2, 5);
        Board board = state.board();
        int greenStep = TestSupport.sameSquare(board, PlayerColor.GREEN, PlayerColor.RED, 20);
        Position p = board.positionOf(PlayerColor.RED, 20);
        TestSupport.piece(state, 1, 1).moveTo(greenStep);
        TestSupport.piece(state, 1, 2).moveTo(greenStep);
        assertTrue(InteractionRules.isDoubleBlock(state, p));
        TestSupport.piece(state, 1, 2).moveTo(greenStep + 3);
        assertFalse(InteractionRules.isDoubleBlock(state, p));
    }

    @Test
    @DisplayName("FR-30: a block on a start square also stops opponents passing it")
    void blockOnStartSquare() {
        GameState state = TestSupport.state(2, 5);
        TestSupport.piece(state, 1, 1).moveTo(0);
        TestSupport.piece(state, 1, 2).moveTo(0); // green block on green's start = red step 13
        TestSupport.piece(state, 0, 1).moveTo(10);
        List<Move> moves = MoveValidator.legalMoves(state, new DiceRoll(5));
        assertEquals(1, moves.size());
        assertEquals(12, moves.get(0).toStep());
    }

    @Test
    @DisplayName("Pieces in the castle are not on any board square")
    void castlePiecesAreOffBoard() {
        GameState state = TestSupport.state(2, 5);
        Position redStart = state.board().positionOf(PlayerColor.RED, 0);
        assertTrue(InteractionRules.piecesAt(state, redStart).isEmpty());
    }
}
