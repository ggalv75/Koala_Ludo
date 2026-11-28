package Model;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MediumMovementTest {
    private GameState stateAt(PlayerColor color, int step) {
        Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(42));
        PlayerColor opponent = color == PlayerColor.RED ? PlayerColor.GREEN : PlayerColor.RED;
        GameState state = new GameState(board, List.of(new Player(0, "Active", color), new Player(1, "Opponent", opponent)));
        state.piecesOf(state.currentPlayer()).get(0).moveTo(step);
        return state;
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    void pieceCanMoveFromOuterToInnerTrack(PlayerColor color) {
        GameState state = stateAt(color, 56);
        Piece piece = state.piecesOf(state.currentPlayer()).get(0);
        List<Move> moves = MoveValidator.legalMoves(state, new DiceRoll(3));
        assertTrue(moves.stream().anyMatch(m -> m.pieceId() == piece.id() && m.toStep() == 59));
        assertEquals(CellType.ARROW, state.board().cellAt(color, 57).type());
        // Random SAFE/SURPRISE/QUESTION cells may occupy this track position.
        // The transition geometry, rather than its random decoration, is fixed.
        Position inner = new MediumBoardLayout().routes().get(color).get(58);
        assertEquals(inner, state.board().positionOf(color, 58));
        assertEquals(CellType.REGULAR, new MediumBoardLayout().fixedCells().get(inner).type());
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    void pieceCanEnterHomePath(PlayerColor color) {
        GameState state = stateAt(color, 108);
        Piece piece = state.piecesOf(state.currentPlayer()).get(0);
        List<Move> moves = MoveValidator.legalMoves(state, new DiceRoll(2));
        assertTrue(moves.stream().anyMatch(m -> m.pieceId() == piece.id() && m.toStep() == 110));
        assertEquals(CellType.ARROW, state.board().cellAt(color, 109).type());
        assertEquals(CellType.HOME, state.board().cellAt(color, 110).type());
        assertEquals(color, state.board().cellAt(color, 110).color());
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    void finishingRequiresExactRoll(PlayerColor color) {
        GameState state = stateAt(color, 114);
        Piece piece = state.piecesOf(state.currentPlayer()).get(0);
        assertTrue(MoveValidator.legalMoves(state, new DiceRoll(1)).stream()
                .anyMatch(m -> m.pieceId() == piece.id() && m.toStep() == 115));
        assertTrue(MoveValidator.legalMoves(state, new DiceRoll(2)).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    void homePathIsPrivateToEachColor(PlayerColor color) {
        Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(42));
        for (int step = 110; step < 115; step++) {
            Position position = board.positionOf(color, step);
            assertEquals(CellType.HOME, board.cellAt(position).type());
            assertEquals(color, board.cellAt(position).color());
            for (PlayerColor other : PlayerColor.values()) {
                if (other != color) {
                    assertFalse(board.route(other).subList(110, 115).contains(position));
                }
            }
        }
    }
}