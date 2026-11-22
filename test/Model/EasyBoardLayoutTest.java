
package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EasyBoardLayoutTest {

    private final BoardLayout layout = new EasyBoardLayout();

    @Test
    void fixedCellsHaveCorrectTypes() {
        Map<Position, Cell> cells = layout.fixedCells();
        assertEquals(72, cells.size());
        assertEquals(4, count(CellType.START));
        assertEquals(4, count(CellType.ARROW));
        assertEquals(20, count(CellType.HOME));
        assertEquals(44, count(CellType.REGULAR));
        assertEquals(0, count(CellType.SAFE));
    }

    @Test
    void allRoutesHaveCorrectLength() {
        for (PlayerColor color : PlayerColor.values()) {
            List<Position> route = layout.routes().get(color);
            assertEquals(56, route.size());
            assertEquals(56, new HashSet<>(route).size());
            assertEquals(CellType.START, layout.fixedCells().get(route.get(0)).type());
            assertEquals(CellType.ARROW, layout.fixedCells().get(route.get(50)).type());
            assertEquals(CellType.HOME, layout.fixedCells().get(route.get(51)).type());
        }
    }

    @Test
    void fixedGeometryIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> layout.fixedCells().clear());
        assertThrows(UnsupportedOperationException.class, () -> layout.routes().clear());
        assertThrows(UnsupportedOperationException.class, () -> layout.routes().get(PlayerColor.RED).clear());
    }

    private long count(CellType type) {
        return layout.fixedCells().values().stream().filter(cell -> cell.type() == type).count();
    }
}
