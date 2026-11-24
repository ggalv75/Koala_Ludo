package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests selection and one-time answer validation without exposing protected answer keys. */
class QuestionServiceTest {

    /** Load the 30-question JSON bank. */
    private QuestionRepository repository() throws IOException {
        return QuestionRepository.fromFile(Path.of("src", "questions.json"), new Random(42));
    }

    /** FR-34: the selected question matches the requested difficulty. */
    @Test void selectsAQuestionOfRequestedLevel() throws IOException {
        QuestionService service = new QuestionService(repository());
        Question question = service.startQuestion(QuestionLevel.HARD);
        assertEquals(QuestionLevel.HARD, question.level());
        assertEquals(question, service.pendingQuestion().orElseThrow());
        assertEquals(4, question.options().size());
    }

    /** Prevent replacing an unanswered question with a new random one. */
    @Test void doesNotReplacePendingQuestion() throws IOException {
        QuestionService service = new QuestionService(repository());
        Question original = service.startQuestion(QuestionLevel.EASY);
        assertThrows(IllegalStateException.class, () -> service.startQuestion(QuestionLevel.MEDIUM));
        assertEquals(original, service.pendingQuestion().orElseThrow());
    }

    /** FR-36: a correct selection returns a correct result and clears the active question. */
    @Test void acceptsCorrectAnswerOnlyOnce() throws IOException {
        QuestionRepository repository = repository();
        QuestionService service = new QuestionService(repository);
        Question question = service.startQuestion(QuestionLevel.MEDIUM);
        int correctIndex = -1;
        for (int i = 0; i < 4; i++) {
            if (repository.isCorrect(question.id(), i)) correctIndex = i;
        }
        assertTrue(correctIndex >= 0);
        QuestionService.AnswerResult result = service.submitAnswer(correctIndex);
        assertEquals(question.id(), result.questionId());
        assertEquals(QuestionLevel.MEDIUM, result.level());
        assertTrue(result.correct());
        assertTrue(service.pendingQuestion().isEmpty());
        assertThrows(IllegalStateException.class, () -> service.submitAnswer(0));
    }

    /** FR-36: a wrong selection is checked without leaking the correct option index. */
    @Test void reportsIncorrectAnswer() throws IOException {
        QuestionRepository repository = repository();
        QuestionService service = new QuestionService(repository);
        Question question = service.startQuestion(QuestionLevel.EASY);
        int wrongIndex = -1;
        for (int i = 0; i < 4; i++) {
            if (!repository.isCorrect(question.id(), i)) {
                wrongIndex = i;
                break;
            }
        }
        assertFalse(service.submitAnswer(wrongIndex).correct());
        assertTrue(service.pendingQuestion().isEmpty());
    }

    /** NFR-01: an invalid answer index does not consume the pending question. */
    @Test void rejectsInvalidOptionWithoutLosingQuestion() throws IOException {
        QuestionService service = new QuestionService(repository());
        Question question = service.startQuestion(QuestionLevel.HARD);
        assertThrows(IllegalArgumentException.class, () -> service.submitAnswer(-1));
        assertThrows(IllegalArgumentException.class, () -> service.submitAnswer(4));
        assertEquals(question, service.pendingQuestion().orElseThrow());
    }

    /** A new question can start after the previous valid answer was submitted. */
    @Test void allowsNextQuestionAfterAnswer() throws IOException {
        QuestionService service = new QuestionService(repository());
        service.startQuestion(QuestionLevel.EASY);
        service.submitAnswer(0);
        assertEquals(QuestionLevel.HARD, service.startQuestion(QuestionLevel.HARD).level());
    }

    /** Disallow answering a question that was never started. */
    @Test void rejectsSubmissionWithoutActiveQuestion() throws IOException {
        QuestionService service = new QuestionService(repository());
        assertTrue(service.pendingQuestion().isEmpty());
        assertThrows(IllegalStateException.class, () -> service.submitAnswer(1));
    }

    /** Reject missing dependencies or missing difficulty. */
    @Test void rejectsNullInputs() throws IOException {
        assertThrows(NullPointerException.class, () -> new QuestionService(null));
        QuestionService service = new QuestionService(repository());
        assertThrows(NullPointerException.class, () -> service.startQuestion(null));
        assertTrue(service.pendingQuestion().isEmpty());
    }
}
