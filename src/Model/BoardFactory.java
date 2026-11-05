package Model;

import java.util.*;
/**
 * Builds the board for a selected difficulty.
 * Iteration 1 supports the Easy board.
 * top-left GREEN, top-right YELLOW,
 * bottom-right BLUE, bottom-left RED.
 */
public final class BoardFactory {
    private static final List<Position> TRACK = createTrack();

    /** Start squares on the shared 52 square track. */
    private static final Map<PlayerColor, Integer> START_INDEX = Map.of(
            PlayerColor.GREEN, 0,
            PlayerColor.YELLOW, 13,
            PlayerColor.BLUE, 26,
            PlayerColor.RED, 39
    );

    /** Private home paths, ordered towards the center. */
    private static final Map<PlayerColor, List<Position>> HOME_PATHS = Map.of(
            PlayerColor.GREEN, line(7, 1, 0, 1),
            PlayerColor.YELLOW, line(1, 7, 1, 0),
            PlayerColor.BLUE, line(7, 13, 0, -1),
            PlayerColor.RED, line(13, 7, -1, 0)
    );

    /** Top-left coordinates of the four 6x6 castles. */
    private static final Map<PlayerColor, Position> CASTLES = Map.of(
            PlayerColor.GREEN, new Position(0, 0),
            PlayerColor.YELLOW, new Position(0, 9),
            PlayerColor.BLUE, new Position(9, 9),
            PlayerColor.RED, new Position(9, 0)
    );

    private static final Position CENTER = new Position(6, 6);

    private BoardFactory() { }

    /**
     * Creates an Easy board with its tracks, home paths, castles,
     * and randomly placed safe squares.
     */
    public static Board create(Difficulty difficulty, Random random) {
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(random, "random");

        if (difficulty.trackSquares() != TRACK.size()) {
            throw new IllegalStateException("Easy track must have " + difficulty.trackSquares() + " squares");
        }

        Map<Integer, Cell> special = new HashMap<>();

        for (PlayerColor color : PlayerColor.values()) {
            special.put(START_INDEX.get(color), new Cell(CellType.START, color));
            special.put(arrowIndex(color, difficulty),new Cell(CellType.ARROW, color));
        }

        placeSafeSquares(special, difficulty.safeSquares(), random);

        Map<Position, Cell> cells = new LinkedHashMap<>();

        for (int i = 0; i < TRACK.size(); i++) {
            cells.put(TRACK.get(i), special.getOrDefault(i, Cell.REGULAR));
        }

        HOME_PATHS.forEach((color, path) -> path.forEach(square ->cells.put(square,new Cell(CellType.HOME, color))));

        Map<PlayerColor, List<Position>> routes = new EnumMap<>(PlayerColor.class);

        for (PlayerColor color : PlayerColor.values()) {
            routes.put(color, buildRoute(color, difficulty));
        }

        return new Board(difficulty, cells, routes, CASTLES, CENTER);
    }

    /**
     * Calculates the arrow square index for a player's color.
     * The arrow marks the transition from the shared track to the home path.
     */
    private static int arrowIndex(PlayerColor color, Difficulty difficulty) {
        return Math.floorMod(START_INDEX.get(color) + difficulty.trackSquares() - 2, TRACK.size());
    }

    /**
     * Randomly places safe squares on available regular track cells.
     * Start squares and arrows are excluded from the selection.
     */
    private static void placeSafeSquares(Map<Integer, Cell> special, int count, Random random) {
        List<Integer> free = new ArrayList<>();

        for (int i = 0; i < TRACK.size(); i++) {
            if (!special.containsKey(i)) {
                free.add(i);
            }
        }

        Collections.shuffle(free, random);

        for (int i = 0; i < count; i++) {
            special.put(free.get(i), new Cell(CellType.SAFE, null));
        }
    }

    /**
     * Builds the complete movement route for a player.
     * The route includes the shared track and the player's private home path.
     */
    private static List<Position> buildRoute(PlayerColor color, Difficulty difficulty) {
        List<Position> route = new ArrayList<>();
        int start = START_INDEX.get(color);

        for (int k = 0; k < difficulty.trackSquares() - 1; k++) {
            route.add(TRACK.get((start + k) % TRACK.size()));
        }

        route.addAll(HOME_PATHS.get(color));
        return route;
    }

    /**
     * Creates a straight sequence of five board positions.
     * The direction is determined by the row and column steps.
     */
    private static List<Position> line(int row, int col, int rowStep, int colStep) {
        List<Position> squares = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            squares.add(new Position(row + i * rowStep, col + i * colStep));
        }

        return List.copyOf(squares);
    }

    /**
     * Defines the 52 squares of the shared Easy board track
     * in their movement order.
     */
    private static List<Position> createTrack() {
        List<Position> t = new ArrayList<>();

        for (int c = 1; c <= 5; c++)
            t.add(new Position(6, c));

        for (int r = 5; r >= 0; r--)
            t.add(new Position(r, 6));

        t.add(new Position(0, 7));

        for (int r = 0; r <= 5; r++)
            t.add(new Position(r, 8));

        for (int c = 9; c <= 14; c++)
            t.add(new Position(6, c));

        t.add(new Position(7, 14));

        for (int c = 14; c >= 9; c--)
            t.add(new Position(8, c));

        for (int r = 9; r <= 14; r++)
            t.add(new Position(r, 8));

        t.add(new Position(14, 7));

        for (int r = 14; r >= 9; r--)
            t.add(new Position(r, 6));

        for (int c = 5; c >= 0; c--)
            t.add(new Position(8, c));

        t.add(new Position(7, 0));
        t.add(new Position(6, 0));

        return List.copyOf(t);
    }
}
