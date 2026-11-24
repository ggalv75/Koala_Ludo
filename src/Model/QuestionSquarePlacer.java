package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** FR-33: places the required question squares without touching fixed or safe squares. */
final class QuestionSquarePlacer {

    /** Prevent utility instantiation. */
    private QuestionSquarePlacer() { }

    /** Place exactly the configured number of questions of each level, without overlap. */
    static void place(Map<Position, Cell> cells, int perLevel, Random random) {
        Objects.requireNonNull(cells, "cells");
        Objects.requireNonNull(random, "random");
        if (perLevel < 0) throw new IllegalArgumentException("Question count cannot be negative");
        if (perLevel == 0) return;
        List<Position> free = new ArrayList<>();
        for (Map.Entry<Position, Cell> entry : cells.entrySet()) {
            Cell cell = entry.getValue();
            if (cell.type() == CellType.REGULAR && cell.color() == null) {
                free.add(entry.getKey());
            }
        }
        if (free.size() < Math.multiplyExact(perLevel, QuestionLevel.values().length)) {
            throw new IllegalArgumentException("Not enough free squares for questions");
        }
        Collections.shuffle(free, random);
        int offset = 0;
        for (QuestionLevel level : QuestionLevel.values()) {
            CellType type = switch (level) {
                case EASY -> CellType.QUESTION_EASY;
                case MEDIUM -> CellType.QUESTION_MEDIUM;
                case HARD -> CellType.QUESTION_HARD;
            };
            for (int i = 0; i < perLevel; i++) {
                cells.put(free.get(offset++), new Cell(type, null));
            }
        }
    }
}
