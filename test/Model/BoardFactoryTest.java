package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for Easy and Medium geometry and safe-square generation (FR-07–FR-14). */
class BoardFactoryTest {
    /** Create an Easy board with a repeatable random layout. */
    private static Board board(long seed) {
        return BoardFactory.create(Difficulty.EASY, new Random(seed));
    }

    /** Create a Medium board with a repeatable random layout. */
    private static Board mediumBoard(long seed) {
        return BoardFactory.create(Difficulty.MEDIUM, new Random(seed));
    }

    /** Return the randomly assigned safe-square positions. */
    private static Set<Position> safeSquares(Board board) {
        Set<Position> positions = new HashSet<>();
        for (Map.Entry<Position, Cell> entry : board.cells().entrySet()) {
            if (entry.getValue().type() == CellType.SAFE) positions.add(entry.getKey());
        }
        return positions;
    }

    /** FR-07–FR-11: verify Easy board dimensions and counts of all cell types. */
    @Test void easyBoardHasRequiredDimensionsAndCellCounts() {
        Board board = board(42);
        assertEquals(Difficulty.EASY, board.difficulty());
        assertEquals(15, board.size());
        assertEquals(4, board.count(CellType.START));
        assertEquals(4, board.count(CellType.ARROW));
        assertEquals(4, board.count(CellType.SAFE));
        assertEquals(20, board.count(CellType.HOME));
        assertEquals(40, board.count(CellType.REGULAR));
        assertEquals(72, board.cells().size());
    }

    /** FR-08: verify each player's start position and color. */
    @Test void startSquaresHaveTheCorrectColorsAndPositions() {
        Board board = board(42);
        Map<PlayerColor, Position> expected = Map.of(
                PlayerColor.GREEN, new Position(6, 1),
                PlayerColor.YELLOW, new Position(1, 8),
                PlayerColor.BLUE, new Position(8, 13),
                PlayerColor.RED, new Position(13, 6));
        for (PlayerColor color : PlayerColor.values()) {
            Position start = board.route(color).get(0);
            assertEquals(expected.get(color), start, "Wrong start position for " + color);
            assertEquals(CellType.START, board.cellAt(start).type());
            assertEquals(color, board.cellAt(start).color());
            assertTrue(board.cellAt(start).isProtected());
        }
    }

    /** FR-09–FR-10: verify arrows, home paths and central finish. */
    @Test void coloredRoutesEnterTheirOwnHomeLanes() {
        Board board = board(42);
        Map<PlayerColor, Position> arrows = Map.of(
                PlayerColor.GREEN, new Position(7, 0),
                PlayerColor.YELLOW, new Position(0, 7),
                PlayerColor.BLUE, new Position(7, 14),
                PlayerColor.RED, new Position(14, 7));
        Map<PlayerColor, Position> firstHome = Map.of(
                PlayerColor.GREEN, new Position(7, 1),
                PlayerColor.YELLOW, new Position(1, 7),
                PlayerColor.BLUE, new Position(7, 13),
                PlayerColor.RED, new Position(13, 7));
        assertEquals(51, board.homePathStart());
        assertEquals(56, board.finishStep());
        for (PlayerColor color : PlayerColor.values()) {
            List<Position> route = board.route(color);
            assertEquals(56, route.size(), "Wrong route length for " + color);
            assertEquals(56, new HashSet<>(route).size(), "Repeated route square for " + color);
            assertEquals(arrows.get(color), route.get(50));
            assertEquals(CellType.ARROW, board.cellAt(route.get(50)).type());
            assertEquals(color, board.cellAt(route.get(50)).color());
            assertEquals(firstHome.get(color), route.get(51));
            for (int step = 51; step <= 55; step++) {
                Cell home = board.cellAt(route.get(step));
                assertEquals(CellType.HOME, home.type(), "Wrong home square at step " + step);
                assertEquals(color, home.color());
                assertTrue(home.isProtected());
            }
            assertNull(board.positionOf(color, board.finishStep()), "Finish is not an extra track cell");
        }
        assertEquals(new Position(6, 6), board.centerCorner());
    }

    /** FR-07: verify the four castle positions. */
    @Test void castlesMatchTheOfficialBoardLayout() {
        Board board = board(42);
        assertEquals(new Position(0, 0), board.castleCorner(PlayerColor.GREEN));
        assertEquals(new Position(0, 9), board.castleCorner(PlayerColor.YELLOW));
        assertEquals(new Position(9, 9), board.castleCorner(PlayerColor.BLUE));
        assertEquals(new Position(9, 0), board.castleCorner(PlayerColor.RED));
    }

    /** R01: verify that random SAFE squares do not change routes or castles. */
    @Test void fixedRoutesAndCastlesRemainStableBetweenGames() {
        Board reference = board(42);
        for (int seed = 0; seed < 50; seed++) {
            Board current = board(seed);
            for (PlayerColor color : PlayerColor.values()) {
                assertEquals(reference.route(color), current.route(color));
                assertEquals(reference.castleCorner(color), current.castleCorner(color));
            }
        }
    }

    /** FR-11/FR-14, R02: verify SAFE positions across 100 games. */
    @Test void safeSquaresAreValidAcrossMultipleGames() {
        for (int seed = 0; seed < 100; seed++) {
            Board board = board(seed);
            Set<Position> track = new HashSet<>();
            for (PlayerColor color : PlayerColor.values()) {
                track.addAll(board.route(color).subList(0, 51));
            }
            Set<Position> safe = safeSquares(board);
            assertEquals(52, track.size(), "Wrong track size at seed " + seed);
            assertEquals(4, board.count(CellType.SAFE), "Wrong SAFE count at seed " + seed);
            assertEquals(4, safe.size(), "Duplicate SAFE position at seed " + seed);
            for (Position square : safe) {
                assertTrue(track.contains(square), "SAFE outside track at seed " + seed);
                assertTrue(board.cellAt(square).isProtected());
                assertNull(board.cellAt(square).color(), "SAFE should have no owner");
            }
            assertEquals(4, board.count(CellType.START));
            assertEquals(4, board.count(CellType.ARROW));
        }
    }

    /** FR-14, R02: verify deterministic seeds and variable SAFE layouts. */
    @Test void randomSafeSquareLayoutIsRepeatableButNotFixed() {
        assertEquals(safeSquares(board(42)), safeSquares(board(42)));
        Set<Set<Position>> layouts = new HashSet<>();
        for (int seed = 0; seed < 30; seed++) {
            layouts.add(safeSquares(board(seed)));
        }
        assertTrue(layouts.size() > 1, "All seeds generated the same SAFE layout");
    }

    /** Verify that board cells and routes cannot be modified externally. */
    @Test void boardDefinitionsCannotBeModifiedExternally() {
        Board board = board(42);
        assertThrows(UnsupportedOperationException.class, () -> board.cells().clear());
        assertThrows(UnsupportedOperationException.class, () -> board.route(PlayerColor.GREEN).clear());
    }

    /** FR-07–FR-14: Medium's six question squares replace six regular track squares. */
    @Test void mediumBoardIsCreatedCorrectly() {
        for (int seed = 0; seed < 30; seed++) {
            Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(seed));
            assertEquals(17, board.size());
            assertEquals(136, board.cells().size());
            assertEquals(4, board.count(CellType.START));
            assertEquals(8, board.count(CellType.ARROW));
            assertEquals(8, board.count(CellType.SAFE));
            assertEquals(20, board.count(CellType.HOME));
            assertEquals(90, board.count(CellType.REGULAR));
            assertEquals(2, board.count(CellType.QUESTION_EASY));
            assertEquals(2, board.count(CellType.QUESTION_MEDIUM));
            assertEquals(2, board.count(CellType.QUESTION_HARD));
            assertEquals(110, board.homePathStart());
            assertEquals(115, board.finishStep());

            for (PlayerColor color : PlayerColor.values()) {
                List<Position> route = board.route(color);
                assertEquals(115, route.size());
                assertEquals(CellType.START, board.cellAt(route.get(0)).type());
                assertEquals(CellType.ARROW, board.cellAt(route.get(57)).type());
                assertEquals(CellType.ARROW, board.cellAt(route.get(109)).type());
                assertEquals(CellType.HOME, board.cellAt(route.get(110)).type());
                assertEquals(color, board.cellAt(route.get(57)).color());
                assertEquals(color, board.cellAt(route.get(109)).color());
            }
        }
    }

    /** Keep Omer's fixed colored Medium squares unchanged by question generation. */
    @Test void mediumColoredSquaresRemainFixed() {
        Map<PlayerColor, Position> expected = Map.of(
                PlayerColor.RED, new Position(7, 2), PlayerColor.GREEN, new Position(2, 9),
                PlayerColor.YELLOW, new Position(9, 14), PlayerColor.BLUE, new Position(14, 7));
        for (int seed = 0; seed < 100; seed++) {
            Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(seed));
            for (PlayerColor color : PlayerColor.values()) {
                Cell cell = board.cellAt(expected.get(color));
                assertEquals(CellType.REGULAR, cell.type());
                assertEquals(color, cell.color());
            }
        }
    }

    /** FR-14: SAFE and QUESTION squares only replace uncolored regular cells without overlap. */
    @Test void mediumSafeSquaresOnlyReplaceUncoloredRegularSquares() {
        Map<Position, Cell> fixedCells = new MediumBoardLayout().fixedCells();
        for (int seed = 0; seed < 100; seed++) {
            Board board = mediumBoard(seed);
            Set<Position> safe = safeSquares(board);
            assertEquals(8, safe.size(), "Wrong SAFE count at seed " + seed);
            for (Position position : safe) {
                assertEquals(Cell.REGULAR, fixedCells.get(position));
                assertNull(board.cellAt(position).color());
                assertTrue(board.cellAt(position).isProtected());
            }
            for (Map.Entry<Position, Cell> entry : fixedCells.entrySet()) {
                CellType actualType = board.cellAt(entry.getKey()).type();
                boolean isQuestion = actualType == CellType.QUESTION_EASY
                        || actualType == CellType.QUESTION_MEDIUM
                        || actualType == CellType.QUESTION_HARD;
                if (isQuestion) {
                    assertEquals(Cell.REGULAR, entry.getValue(), "Questions can only replace regular squares");
                } else if (!safe.contains(entry.getKey())) {
                    assertEquals(entry.getValue(), board.cellAt(entry.getKey()));
                }
            }
        }
    }

    /** Verify that random SAFE squares remain deterministic and vary with the seed. */
    @Test void mediumSafeSquaresAreRepeatableButRandom() {
        assertEquals(safeSquares(mediumBoard(42)), safeSquares(mediumBoard(42)));
        Set<Set<Position>> layouts = new HashSet<>();
        for (int seed = 0; seed < 30; seed++) {
            layouts.add(safeSquares(mediumBoard(seed)));
        }
        assertTrue(layouts.size() > 1, "SAFE positions did not change between games");
    }
}
