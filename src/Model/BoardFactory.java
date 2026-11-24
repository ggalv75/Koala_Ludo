package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** Preserve Omer's Factory Method and extend Medium with question-cell placement. */
public abstract class BoardFactory {
    /** Create concrete factories via the static entry point. */
    protected BoardFactory() { }

    /** Subclasses supply the geometry of their respective board level. */
    protected abstract BoardLayout createLayout();

    /** Create Easy or Medium boards without exposing their layout classes. */
    public static Board create(Difficulty difficulty, Random random) {
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(random, "random");
        BoardFactory factory = switch (difficulty) {
            case EASY -> new EasyBoardFactory();
            case MEDIUM -> new MediumBoardFactory();
        };
        return factory.build(difficulty, random);
    }

    /** Construct a complete board and place the random special squares. */
    private Board build(Difficulty difficulty, Random random) {
        BoardLayout layout = createLayout();
        Map<Position, Cell> cells = new LinkedHashMap<>(layout.fixedCells());
        long trackCount = cells.values().stream().filter(cell -> cell.type() != CellType.HOME).count();
        if (trackCount != difficulty.trackSquares()) {
            throw new IllegalStateException("Invalid track size: " + trackCount);
        }
        placeSafeSquares(cells, difficulty.safeSquares(), random);
        QuestionSquarePlacer.place(cells, difficulty.questionSquaresPerLevel(), random);
        return new Board(difficulty, cells, layout.routes(), layout.castleCorners(), layout.centerCorner());
    }

    /** Randomly place protected cells only on uncolored regular track squares. */
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
