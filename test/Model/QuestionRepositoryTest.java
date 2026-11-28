package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Verifies the initial 30-question bank without exposing answer keys in test sources. */
class QuestionRepositoryTest {

    /** Load the JSON question bank. */
    private QuestionRepository repository() throws IOException {
        return QuestionRepository.fromFile(Path.of("src", "questions.json"), new Random(42));
    }

    /** FR-60: the initial database contains 30 questions, ten at each level. */
    @Test void loadsAllThirtyQuestions() throws IOException {
        QuestionRepository repo = repository();
        assertEquals(30, repo.size());
        assertEquals(10, repo.count(QuestionLevel.EASY));
        assertEquals(10, repo.count(QuestionLevel.MEDIUM));
        assertEquals(10, repo.count(QuestionLevel.HARD));
        for (Question q : repo.findAll()) {
            assertEquals(4, q.options().size());
            assertFalse(q.text().isBlank());
            assertFalse(q.topic().isBlank());
        }
    }

    /** FR-34: random selection is limited to the requested question level. */
    @Test void randomQuestionAlwaysMatchesLevel() throws IOException {
        QuestionRepository repo = repository();
        for (QuestionLevel level : QuestionLevel.values()) {
            for (int i = 0; i < 100; i++) {
                assertEquals(level, repo.findRandom(level).orElseThrow().level());
            }
        }
    }

    /** FR-36: each question has one correct answer and three incorrect ones. */
    @Test void exactlyOneCorrectAnswerPerQuestion() throws IOException {
        QuestionRepository repo = repository();
        for (Question q : repo.findAll()) {
            int correctCount = 0;
            for (int i = 0; i < 4; i++) {
                if (repo.isCorrect(q.id(), i)) correctCount++;
            }
            assertEquals(1, correctCount, "Question " + q.id());
        }
    }

    /** NFR-01: malformed answer selections must not be treated as valid. */
    @Test void rejectsOutOfRangeAnswer() throws IOException {
        QuestionRepository repo = repository();
        assertThrows(IllegalArgumentException.class, () -> repo.isCorrect(1, -1));
        assertThrows(IllegalArgumentException.class, () -> repo.isCorrect(1, 4));
        assertThrows(IllegalArgumentException.class, () -> repo.isCorrect(999, 0));
    }

    /** The View cannot mutate options inside the repository. */
    @Test void publicQuestionDataIsImmutable() throws IOException {
        Question q = repository().findById(1).orElseThrow();
        List<String> options = q.options();
        assertThrows(UnsupportedOperationException.class, () -> options.set(0, "changed"));
    }
}
