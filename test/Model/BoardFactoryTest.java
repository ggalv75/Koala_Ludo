package Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Board structure, FR-07 to FR-11 and FR-14 (SDP 5.2.1: Omer). */
class BoardFactoryTest {
    private final Board board = BoardFactory.create(Difficulty.EASY, new Random(42));

    @Test
    @DisplayName("FR-07: 15x15 board with a 52-square track and 4 castles")
    void easyBoardStructure() {
        assertEquals(15, board.size());
        long trackSquares = board.cells().values().stream().filter(c -> c.type() != CellType.HOME).count();
        assertEquals(52, trackSquares);
        for (PlayerColor color : PlayerColor.values()) assertNotNull(board.castleCorner(color));
    }

    @Test
    @DisplayName("FR-08: one protected start square (#) per colour at step 0 of its route")
    void startSquares() {
        assertEquals(4, board.count(CellType.START));
        for (PlayerColor color : PlayerColor.values()) {
            Cell start = board.cellAt(color, 0);
            assertEquals(CellType.START, start.type());
            assertEquals(color, start.color());
            assertTrue(start.isProtected());
        }
    }

    @Test
    @DisplayName("FR-09: each route ends with the colour's protected home path")
    void homePaths() {
        assertEquals(20, board.count(CellType.HOME));
        for (PlayerColor color : PlayerColor.values()) {
            for (int step = board.homePathStart(); step < board.finishStep(); step++) {
                Cell cell = board.cellAt(color, step);
                assertEquals(CellType.HOME, cell.type());
                assertEquals(color, cell.color());
                assertTrue(cell.isProtected());
            }
            assertNull(board.positionOf(color, board.finishStep()), "The finish area is not a square");
        }
    }

    @Test
    @DisplayName("FR-10: 4 arrows, each the last shared square before its colour's home path")
    void arrows() {
        assertEquals(4, board.count(CellType.ARROW));
        for (PlayerColor color : PlayerColor.values()) {
            Cell arrow = board.cellAt(color, board.homePathStart() - 1);
            assertEquals(CellType.ARROW, arrow.type());
            assertEquals(color, arrow.color());
        }
    }

    @Test
    @DisplayName("FR-11 / FR-14: always 4 protected safe squares, never on start, arrow or home squares")
    void safeSquaresOnRegularTrackSquaresOnly() {
        for (int seed = 0; seed < 300; seed++) {
            Board b = BoardFactory.create(Difficulty.EASY, new Random(seed));
            assertEquals(4, b.count(CellType.SAFE), "seed " + seed);
            assertEquals(4, b.count(CellType.START), "seed " + seed);
            assertEquals(4, b.count(CellType.ARROW), "seed " + seed);
            b.cells().values().stream().filter(c -> c.type() == CellType.SAFE)
                    .forEach(c -> assertTrue(c.isProtected()));
        }
    }

    @Test
    @DisplayName("FR-14: safe-square locations change between games")
    void safeSquaresAreRandom() {
        Set<Set<Position>> layouts = new HashSet<>();
        for (int seed = 0; seed < 20; seed++) {
            Board b = BoardFactory.create(Difficulty.EASY, new Random(seed));
            Set<Position> safe = new HashSet<>();
            b.cells().forEach((p, c) -> { if (c.type() == CellType.SAFE) safe.add(p); });
            layouts.add(safe);
        }
        assertTrue(layouts.size() > 10);
    }

    @Test
    @DisplayName("All colours share the same physical track (needed for captures and blocks)")
    void routesShareTheTrack() {
        assertEquals(board.positionOf(PlayerColor.GREEN, 0), board.positionOf(PlayerColor.RED, 13));
        assertEquals(board.positionOf(PlayerColor.YELLOW, 0), board.positionOf(PlayerColor.RED, 26));
        assertEquals(board.positionOf(PlayerColor.BLUE, 0), board.positionOf(PlayerColor.RED, 39));
        for (PlayerColor color : PlayerColor.values()) assertEquals(56, board.route(color).size());
    }
}
