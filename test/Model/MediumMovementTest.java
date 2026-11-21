package Model;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MediumMovementTest {
    private GameState stateAt(int step) {
        Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(42));
        GameState state = new GameState(board, List.of(new Player(0, "Red", PlayerColor.RED),new Player(1, "Green", PlayerColor.GREEN)));
        state.piecesOf(state.currentPlayer()).get(0).moveTo(step);
        return state;
    }

    @Test
    void pieceCanMoveFromOuterToInnerTrack() {
        GameState state = stateAt(56);
        Piece piece = state.piecesOf(state.currentPlayer()).get(0);
        List<Move> moves = MoveValidator.legalMoves(state, new DiceRoll(3));
        assertTrue(moves.stream().anyMatch(m -> m.pieceId() == piece.id() && m.toStep() == 59));
        assertEquals(new Position(7, 0), state.board().positionOf(PlayerColor.RED, 57));
        assertEquals(new Position(7, 1), state.board().positionOf(PlayerColor.RED, 58));
    }

    @Test
    void pieceCanEnterHomePath() {
        GameState state = stateAt(108);
        Piece piece = state.piecesOf(state.currentPlayer()).get(0);
        List<Move> moves = MoveValidator.legalMoves(state, new DiceRoll(2));
        assertTrue(moves.stream().anyMatch(m -> m.pieceId() == piece.id() && m.toStep() == 110));
        assertEquals(CellType.ARROW, state.board().cellAt(PlayerColor.RED, 109).type());
        assertEquals(CellType.HOME, state.board().cellAt(PlayerColor.RED, 110).type());
    }

    @Test
    void finishingRequiresExactRoll() {
        GameState state = stateAt(114);
        Piece piece = state.piecesOf(state.currentPlayer()).get(0);
        assertTrue(MoveValidator.legalMoves(state, new DiceRoll(1)).stream().anyMatch(m -> m.pieceId() == piece.id() && m.toStep() == 115));
        assertTrue(MoveValidator.legalMoves(state, new DiceRoll(2)).isEmpty());
    }
}