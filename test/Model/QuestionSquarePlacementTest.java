package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** FR-33: questions on Medium use correct colors and never replace protected cells. */
class QuestionSquarePlacementTest {

    /** Medium has exactly two questions at each of the three levels. */
    @Test void mediumContainsTwoQuestionsOfEachLevel() {
        for (long seed = 0; seed < 30; seed++) {
            Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(seed));
            assertEquals(2, board.count(CellType.QUESTION_EASY));
            assertEquals(2, board.count(CellType.QUESTION_MEDIUM));
            assertEquals(2, board.count(CellType.QUESTION_HARD));
            assertEquals(8, board.count(CellType.SAFE));
            assertEquals(90, board.count(CellType.REGULAR));
            assertEquals(136, board.cells().size());
            assertEquals(4, board.count(CellType.START));
            assertEquals(8, board.count(CellType.ARROW));
            assertEquals(20, board.count(CellType.HOME));
        }
    }

    /** The board must be repeatable from a seed and vary across games. */
    @Test void mediumQuestionPositionsAreRandomAndRepeatable() {
        assertEquals(questionPositions(77), questionPositions(77));
        Set<Set<Position>> layouts = new HashSet<>();
        for (int seed = 0; seed < 25; seed++) layouts.add(questionPositions(seed));
        assertTrue(layouts.size() > 1);
    }

    /** The Easy board is not affected by Medium question generation. */
    @Test void easyHasNoQuestions() {
        Board board = BoardFactory.create(Difficulty.EASY, new Random(42));
        assertEquals(0, board.count(CellType.QUESTION_EASY));
        assertEquals(0, board.count(CellType.QUESTION_MEDIUM));
        assertEquals(0, board.count(CellType.QUESTION_HARD));
    }

    /** Return question squares independently of the generated board's other cells. */
    private static Set<Position> questionPositions(long seed) {
        Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(seed));
        Set<Position> found = new HashSet<>();
        board.cells().forEach((pos, cell) -> {
            if (cell.type() == CellType.QUESTION_EASY || cell.type() == CellType.QUESTION_MEDIUM
                    || cell.type() == CellType.QUESTION_HARD) found.add(pos);
        });
        assertEquals(6, found.size());
        return found;
    }
}
