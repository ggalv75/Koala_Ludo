package Model;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MediumBoardLayoutTest {
    private final BoardLayout layout = new MediumBoardLayout();
    
    private long count(CellType type) {
        return layout.fixedCells().values().stream().filter(cell -> cell.type() == type).count();
    }

    @Test
    void mediumBoardHasCorrectCellCounts() {
        assertEquals(136, layout.fixedCells().size());
        assertEquals(4, count(CellType.START));
        assertEquals(8, count(CellType.ARROW));
        assertEquals(20, count(CellType.HOME));
        assertEquals(104, count(CellType.REGULAR));
        assertEquals(0, count(CellType.SAFE));
    }

    @Test
    void allRoutesHaveCorrectLengthAndNoDuplicates() {
        for (PlayerColor color : PlayerColor.values()) {
            List<Position> route = layout.routes().get(color);
            assertNotNull(route);
            assertEquals(115, route.size());
            assertEquals(115, new HashSet<>(route).size());
            for (Position position : route) {
                assertTrue(layout.fixedCells().containsKey(position));
            }
        }
    }

    @Test
    void routesPassThroughBothArrowsAndEnterHome() {
        for (PlayerColor color : PlayerColor.values()) {
            List<Position> route = layout.routes().get(color);
            assertEquals(CellType.START, layout.fixedCells().get(route.get(0)).type());
            assertEquals(CellType.ARROW, layout.fixedCells().get(route.get(57)).type());
            assertEquals(CellType.ARROW, layout.fixedCells().get(route.get(109)).type());
            assertEquals(CellType.HOME, layout.fixedCells().get(route.get(110)).type());
            assertEquals(color, layout.fixedCells().get(route.get(57)).color());
            assertEquals(color, layout.fixedCells().get(route.get(109)).color());
        }
    }

    @Test
    void routesHaveExpectedStartPositions() {
        Map<PlayerColor, Position> expected = Map.of(
        		PlayerColor.RED, new Position(0, 0), PlayerColor.GREEN, new Position(0, 16), 
        		PlayerColor.YELLOW, new Position(16, 16), PlayerColor.BLUE, new Position(16, 0));
        for (PlayerColor color : PlayerColor.values()) {
            assertEquals(expected.get(color), layout.routes().get(color).get(0));
        }
    }

    @Test
    void boardLayoutIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> layout.fixedCells().clear());
        assertThrows(UnsupportedOperationException.class, () -> layout.routes().clear());
        assertThrows(UnsupportedOperationException.class, () -> layout.routes().get(PlayerColor.RED).clear());
    }
    
	@Test
	void mediumRoutesUseCorrectTrackTransitions() {
	    Map<PlayerColor, Position> outerArrows = Map.of(
	    		PlayerColor.RED, new Position(7, 0), PlayerColor.GREEN, new Position(0, 9), 
	    		PlayerColor.YELLOW, new Position(9, 16), PlayerColor.BLUE, new Position(16, 7));
	    Map<PlayerColor, Position> innerEntries = Map.of(
	    		PlayerColor.RED, new Position(7, 1), PlayerColor.GREEN, new Position(1, 9), 
	    		PlayerColor.YELLOW, new Position(9, 15), PlayerColor.BLUE, new Position(15, 7));
	    Map<PlayerColor, Position> innerArrows = Map.of(
	    		PlayerColor.RED, new Position(8, 1), PlayerColor.GREEN, new Position(1, 8), 
	    		PlayerColor.YELLOW, new Position(8, 15), PlayerColor.BLUE, new Position(15, 8));
	    Map<PlayerColor, Position> firstHome = Map.of(
	    		PlayerColor.RED, new Position(8, 2), PlayerColor.GREEN, new Position(2, 8), 
	    		PlayerColor.YELLOW, new Position(8, 14), PlayerColor.BLUE, new Position(14, 8));
	
	    for (PlayerColor color : PlayerColor.values()) {
	        List<Position> route = layout.routes().get(color);
	        assertEquals(outerArrows.get(color), route.get(57));
	        assertEquals(innerEntries.get(color), route.get(58));
	        assertEquals(innerArrows.get(color), route.get(109));
	        assertEquals(firstHome.get(color), route.get(110));
	    }
	}
}