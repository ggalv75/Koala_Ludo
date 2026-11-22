package Model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Factory Method creator for Easy and Medium game boards.
 * Builds board layouts and places random SAFE squares.
 * Implements board construction requirements FR-07–FR-14.
 */
public abstract class BoardFactory {
    protected BoardFactory() {}

    /**
     * Factory Method implemented by concrete creators.
     */
    protected abstract BoardLayout createLayout();

    /**
     * Existing entry point, preserved for backward compatibility.
     */
    public static Board create(Difficulty difficulty, Random random) {
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(random, "random");
        BoardFactory factory = switch (difficulty) {
        case EASY -> new EasyBoardFactory();
        case MEDIUM -> new MediumBoardFactory();
        };
        return factory.build(difficulty, random);
    }

    /**
     * Shared board construction process.
     */
    private Board build(Difficulty difficulty, Random random) {
        BoardLayout layout = createLayout();
        Map<Position, Cell> cells = new LinkedHashMap<>(layout.fixedCells());

        long trackCount = cells.values().stream().filter(cell -> cell.type() != CellType.HOME).count();
        if (trackCount != difficulty.trackSquares()) {
            throw new IllegalStateException("Invalid track size: " + trackCount);
        }

        placeSafeSquares(cells, difficulty.safeSquares(), random);

        return new Board(difficulty, cells, layout.routes(), layout.castleCorners(), layout.centerCorner());
    }

    /**
     * Places safe squares on distinct regular track positions.
     */
    private static void placeSafeSquares(Map<Position, Cell> cells, int count, Random random) {
        List<Position> free = new ArrayList<>();
        for (Map.Entry<Position, Cell> entry : cells.entrySet()) {
        	if (entry.getValue().type() == CellType.REGULAR && entry.getValue().color() == null) {
        	    free.add(entry.getKey());
        	}
        }
        if (count < 0 || count > free.size()) {
            throw new IllegalArgumentException("Invalid safe square count: " + count);
        }
        Collections.shuffle(free, random);
        for (int i = 0; i < count; i++) {
            cells.put(free.get(i), new Cell(CellType.SAFE, null));
        }
    }
}
