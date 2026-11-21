
package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the fixed geometry and movement routes of the Easy board.
 * Random special squares are added later by the board factory.
 */
final class EasyBoardLayout implements BoardLayout {

    private static final List<Position> TRACK = createTrack();
    private static final Map<PlayerColor, Integer> START_INDEX = Map.of(PlayerColor.GREEN, 0, PlayerColor.YELLOW, 13, PlayerColor.BLUE, 26, PlayerColor.RED, 39);
    private static final Map<PlayerColor, List<Position>> HOME_PATHS = Map.of(
    		PlayerColor.GREEN, line(7, 1, 0, 1), PlayerColor.YELLOW, line(1, 7, 1, 0),
    		PlayerColor.BLUE, line(7, 13, 0, -1), PlayerColor.RED, line(13, 7, -1, 0));
    private static final Map<PlayerColor, Position> CASTLES = Map.of(
    		PlayerColor.GREEN, new Position(0, 0), PlayerColor.YELLOW, new Position(0, 9),
    		PlayerColor.BLUE, new Position(9, 9), PlayerColor.RED, new Position(9, 0));
    private static final Position CENTER = new Position(6, 6);
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
     * Creates the board cells without randomly placed SAFE squares.
     */
    private static Map<Position, Cell> createFixedCells() {
        Map<Position, Cell> cells = new LinkedHashMap<>();
        for (Position position : TRACK) {
            cells.put(position, Cell.REGULAR);
        }
        for (PlayerColor color : PlayerColor.values()) {
            int startIndex = START_INDEX.get(color);
            int arrowIndex = Math.floorMod(startIndex + TRACK.size() - 2, TRACK.size());
            cells.put(TRACK.get(startIndex), new Cell(CellType.START, color));
            cells.put(TRACK.get(arrowIndex), new Cell(CellType.ARROW, color));
            for (Position position : HOME_PATHS.get(color)) {
                cells.put(position, new Cell(CellType.HOME, color));
            }
        }
        return Collections.unmodifiableMap(cells);
    }

    /**
     * Builds the movement route for each player color.
     */
    private static Map<PlayerColor, List<Position>> createRoutes() {
        Map<PlayerColor, List<Position>> routes = new EnumMap<>(PlayerColor.class);
        for (PlayerColor color : PlayerColor.values()) {
            List<Position> route = new ArrayList<>();
            int startIndex = START_INDEX.get(color);
            for (int step = 0; step < TRACK.size() - 1; step++) {
                int index = (startIndex + step) % TRACK.size();
                route.add(TRACK.get(index));
            }
            route.addAll(HOME_PATHS.get(color));
            routes.put(color, List.copyOf(route));
        }
        return Collections.unmodifiableMap(routes);
    }

    /**
     * Creates five consecutive home-path positions.
     */
    private static List<Position> line(int row, int col, int rowStep, int colStep) {
        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            positions.add(new Position(row + i * rowStep, col + i * colStep));
        }
        return List.copyOf(positions);
    }

    /**
     * Creates the 52 shared track squares in clockwise order.
     */
    private static List<Position> createTrack() {
        List<Position> track = new ArrayList<>();
        for (int c = 1; c <= 5; c++) {
            track.add(new Position(6, c));
        }
        for (int r = 5; r >= 0; r--) {
            track.add(new Position(r, 6));
        }
        track.add(new Position(0, 7));
        for (int r = 0; r <= 5; r++) {
            track.add(new Position(r, 8));
        }
        for (int c = 9; c <= 14; c++) {
            track.add(new Position(6, c));
        }
        track.add(new Position(7, 14));
        for (int c = 14; c >= 9; c--) {
            track.add(new Position(8, c));
        }
        for (int r = 9; r <= 14; r++) {
            track.add(new Position(r, 8));
        }
        track.add(new Position(14, 7));
        for (int r = 14; r >= 9; r--) {
            track.add(new Position(r, 6));
        }
        for (int c = 5; c >= 0; c--) {
            track.add(new Position(8, c));
        }
        track.add(new Position(7, 0));
        track.add(new Position(6, 0));
        return List.copyOf(track);
    }
}
