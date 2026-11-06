package Model;

import java.util.*;

/**
 * Immutable board of one game: the squares and the route each colour follows.
 * A piece's progress is a "step" on its own route: 0 is the start square,
 * {@link #homePathStart()} is the first home square and {@link #finishStep()} is the finish area.
 */
public final class Board {
    private final Difficulty difficulty;
    private final Map<Position, Cell> cells;
    private final Map<PlayerColor, List<Position>> routes;
    private final Map<PlayerColor, Position> castleCorners;
    private final Position centerCorner;

    /** Build an immutable board, routes, castles and center from its configuration. */
    Board(Difficulty difficulty, Map<Position, Cell> cells, Map<PlayerColor, List<Position>> routes,
          Map<PlayerColor, Position> castleCorners, Position centerCorner) {
        this.difficulty = difficulty;
        this.cells = Collections.unmodifiableMap(new LinkedHashMap<>(cells));
        EnumMap<PlayerColor, List<Position>> copy = new EnumMap<>(PlayerColor.class);
        routes.forEach((color, route) -> copy.put(color, List.copyOf(route)));
        this.routes = Collections.unmodifiableMap(copy);
        this.castleCorners = Collections.unmodifiableMap(new EnumMap<>(castleCorners));
        this.centerCorner = centerCorner;
    }

    /** Return the difficulty configuration used to build the board. */
    public Difficulty difficulty() { return difficulty; }
    /** Return the number of rows and columns on this board. */
    public int size() { return difficulty.boardSize(); }
    /** Expose the immutable map of physical board squares. */
    public Map<Position, Cell> cells() { return cells; }
    /** Look up a square by its position or route step. */
    public Cell cellAt(Position position) { return cells.get(position); }
    /** Return the immutable movement route for one player color. */
    public List<Position> route(PlayerColor color) { return routes.get(color); }

    /** Number of squares a piece passes on its route (shared track part plus home path). */
    public int routeLength() { return routes.get(PlayerColor.RED).size(); }

    /** Step of a piece that has entered the finish area in the centre. */
    public int finishStep() { return routeLength(); }

    /** First step that is on the colour's home path. */
    public int homePathStart() { return routeLength() - difficulty.homePathSquares(); }

    /** Square at the given step of a colour's route, or null for the castle and the finish area. */
    public Position positionOf(PlayerColor color, int step) {
        List<Position> route = routes.get(color);
        return step >= 0 && step < route.size() ? route.get(step) : null;
    }

    /** Look up a square by its position or route step. */
    public Cell cellAt(PlayerColor color, int step) {
        Position position = positionOf(color, step);
        return position == null ? null : cells.get(position);
    }

    /** Top-left square of the colour's 6x6 castle. */
    public Position castleCorner(PlayerColor color) { return castleCorners.get(color); }

    /** Top-left square of the 3x3 finish area. */
    public Position centerCorner() { return centerCorner; }

    /** Count board squares of the requested type. */
    public long count(CellType type) {
        return cells.values().stream().filter(cell -> cell.type() == type).count();
    }
}
