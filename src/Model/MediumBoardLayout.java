package Model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the fixed geometry and movement routes of the Medium board.
 * Random special squares are added later by the board factory.
 */
final class MediumBoardLayout implements BoardLayout {
    private static final int SIZE = 17;
    private static final List<Position> OUTER_TRACK = createOuterTrack();
    private static final List<Position> INNER_TRACK = createInnerTrack();
    private static final Map<PlayerColor, Position> STARTS = Map.of(
    		PlayerColor.RED, new Position(0, 0), PlayerColor.GREEN, new Position(0, 16),
    		PlayerColor.YELLOW, new Position(16, 16), PlayerColor.BLUE, new Position(16, 0));
    private static final Map<PlayerColor, Position> OUTER_ARROWS = Map.of(
    		PlayerColor.RED, new Position(7, 0), PlayerColor.GREEN, new Position(0, 9), 
    		PlayerColor.YELLOW, new Position(9, 16), PlayerColor.BLUE, new Position(16, 7));
    private static final Map<PlayerColor, Position> INNER_ENTRIES = Map.of(
    		PlayerColor.RED, new Position(7, 1), PlayerColor.GREEN, new Position(1, 9), 
    		PlayerColor.YELLOW, new Position(9, 15), PlayerColor.BLUE, new Position(15, 7));
    private static final Map<PlayerColor, Position> INNER_ARROWS = Map.of(
    		PlayerColor.RED, new Position(8, 1), PlayerColor.GREEN, new Position(1, 8), 
    		PlayerColor.YELLOW, new Position(8, 15), PlayerColor.BLUE, new Position(15, 8));
    private static final Map<PlayerColor, List<Position>> HOME_PATHS = Map.of(
    		PlayerColor.RED, line(8, 2, 0, 1), PlayerColor.GREEN, line(2, 8, 1, 0), 
    		PlayerColor.YELLOW, line(8, 14, 0, -1), PlayerColor.BLUE, line(14, 8, -1, 0));
    private static final Map<PlayerColor, Position> CASTLES = Map.of(
    		PlayerColor.RED, new Position(1, 1), PlayerColor.GREEN, new Position(1, 10), 
    		PlayerColor.YELLOW, new Position(10, 10), PlayerColor.BLUE, new Position(10, 1));
    private static final Map<PlayerColor, Position> COLORED_INNER = Map.of(
    		PlayerColor.RED, new Position(7, 2), PlayerColor.GREEN, new Position(2, 9), 
    		PlayerColor.YELLOW, new Position(9, 14), PlayerColor.BLUE, new Position(14, 7));
    private static final Position CENTER = new Position(7, 7);
    private static final Map<Position, Cell> FIXED_CELLS = createFixedCells();
    private static final Map<PlayerColor, List<Position>> ROUTES = createRoutes();

    @Override
    public Map<Position, Cell> fixedCells() {
        return FIXED_CELLS;
    }

    @Override
    public Map<PlayerColor, List<Position>> routes() {
        return ROUTES;
    }

    @Override
    public Map<PlayerColor, Position> castleCorners() {
        return CASTLES;
    }

    @Override
    public Position centerCorner() {
        return CENTER;
    }

    /**
     * Creates the fixed board cells without random special squares.
     */
    private static Map<Position, Cell> createFixedCells() {
        Map<Position, Cell> cells = new LinkedHashMap<>();
        for (Position position : OUTER_TRACK) {
            cells.put(position, Cell.REGULAR);
        }
        for (Position position : INNER_TRACK) {
            cells.put(position, Cell.REGULAR);
        }
        for (PlayerColor color : PlayerColor.values()) {
            for (Position position : HOME_PATHS.get(color)) {
                cells.put(position, new Cell(CellType.HOME, color));
            }
            cells.put(STARTS.get(color), new Cell(CellType.START, color));
            cells.put(OUTER_ARROWS.get(color), new Cell(CellType.ARROW, color));
            cells.put(INNER_ARROWS.get(color), new Cell(CellType.ARROW, color));
            cells.put(COLORED_INNER.get(color), new Cell(CellType.REGULAR, color));
        }
        return Collections.unmodifiableMap(cells);
    }

    /**
     * Builds each player's outer, inner and home routes.
     */
    private static Map<PlayerColor, List<Position>> createRoutes() {
        Map<PlayerColor, List<Position>> routes = new EnumMap<>(PlayerColor.class);
        for (PlayerColor color : PlayerColor.values()) {
            List<Position> route = new ArrayList<>();
            int outerStart = OUTER_TRACK.indexOf(STARTS.get(color));
            int outerArrow = OUTER_TRACK.indexOf(OUTER_ARROWS.get(color));
            int innerStart = INNER_TRACK.indexOf(INNER_ENTRIES.get(color));
            if (outerStart < 0 || outerArrow < 0 || innerStart < 0) {
                throw new IllegalStateException("Missing Medium track position for " + color);
            }
            int outerSteps = Math.floorMod(outerArrow - outerStart, OUTER_TRACK.size());
            for (int step = 0; step <= outerSteps; step++) {
                route.add(OUTER_TRACK.get((outerStart + step) % OUTER_TRACK.size()));
            }
            for (int step = 0; step < INNER_TRACK.size(); step++) {
                route.add(INNER_TRACK.get((innerStart + step) % INNER_TRACK.size()));
            }
            if (!route.get(route.size() - 1).equals(INNER_ARROWS.get(color))) {
                throw new IllegalStateException("Wrong inner arrow for " + color);
            }
            route.addAll(HOME_PATHS.get(color));
            routes.put(color, List.copyOf(route));
        }
        return Collections.unmodifiableMap(routes);
    }

    /**
     * Creates the 64 outer track squares in clockwise order.
     */
    private static List<Position> createOuterTrack() {
        List<Position> track = new ArrayList<>(64);
        for (int col = 0; col < SIZE; col++) {
            track.add(new Position(0, col));
        }
        for (int row = 1; row < SIZE; row++) {
            track.add(new Position(row, SIZE - 1));
        }
        for (int col = SIZE - 2; col >= 0; col--) {
            track.add(new Position(SIZE - 1, col));
        }
        for (int row = SIZE - 2; row > 0; row--) {
            track.add(new Position(row, 0));
        }
        return List.copyOf(track);
    }

    /**
     * Creates the 52 inner track squares.
     * The geometry corresponds to the Easy track shifted inward by one cell.
     */
    private static List<Position> createInnerTrack() {
        List<Position> track = new ArrayList<>(52);
        for (int col = 2; col <= 6; col++) {
            track.add(new Position(7, col));
        }
        for (int row = 6; row >= 1; row--) {
            track.add(new Position(row, 7));
        }
        track.add(new Position(1, 8));
        for (int row = 1; row <= 6; row++) {
            track.add(new Position(row, 9));
        }
        for (int col = 10; col <= 15; col++) {
            track.add(new Position(7, col));
        }
        track.add(new Position(8, 15));
        for (int col = 15; col >= 10; col--) {
            track.add(new Position(9, col));
        }
        for (int row = 10; row <= 15; row++) {
            track.add(new Position(row, 9));
        }
        track.add(new Position(15, 8));
        for (int row = 15; row >= 10; row--) {
            track.add(new Position(row, 7));
        }
        for (int col = 6; col >= 1; col--) {
            track.add(new Position(9, col));
        }
        track.add(new Position(8, 1));
        track.add(new Position(7, 1));
        return List.copyOf(track);
    }

    /**
     * Creates the five home squares following a player's inner arrow.
     */
    private static List<Position> line(int row, int col, int rowStep, int colStep) {
        List<Position> positions = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            positions.add(new Position(row + i * rowStep, col + i * colStep));
        }
        return List.copyOf(positions);
    }
}