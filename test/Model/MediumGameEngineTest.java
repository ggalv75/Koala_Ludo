package Model;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MediumGameEngineTest {
    @Test
    void engineInitializesMediumGameCorrectly() {
        GameEngine engine = new GameEngine(Difficulty.MEDIUM,List.of("Red", "Green"),List.of(PlayerColor.RED, PlayerColor.GREEN));

        GameState state = engine.state();
        Board board = state.board();

        assertEquals(Difficulty.MEDIUM, state.difficulty());
        assertEquals(17, board.size());
        assertEquals(115, board.finishStep());
        assertEquals(8, board.count(CellType.ARROW));
        assertEquals(8, board.count(CellType.SAFE));
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
    }

	@Test
	void engineMovesPieceFromOuterToInnerTrack() {
		Dice fixedDice = new Dice() {
		    @Override
		    public DiceRoll roll() {
		        return new DiceRoll(3);
		    }
		
		    @Override
		    public int rollMultiplier(MultiplierDie die) {
		        assertEquals(MultiplierDie.BOOST, die);
		        return 2;
		    }
		};
		
	    GameEngine engine = new GameEngine(Difficulty.MEDIUM, List.of("Red", "Green"), List.of(PlayerColor.RED, PlayerColor.GREEN),
	    		new java.util.Random(42), fixedDice, java.time.Clock.systemUTC());
	
	    Piece piece = engine.state().piece(0);
	    piece.moveTo(56);
	
	    engine.rollDice();
	    assertTrue(engine.movePiece(piece.id()));
	    assertEquals(59, piece.step());
	    assertEquals(new Position(7, 2), engine.state().positionOf(piece));
	}
}