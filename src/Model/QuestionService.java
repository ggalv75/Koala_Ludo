package Model;

import java.util.Objects;
import java.util.Optional;

/** Coordinates one question at a time without exposing answer keys or depending on the View. */
public final class QuestionService {
    private final QuestionRepository repository;
    private Question pendingQuestion;

    /** Use the existing repository to select and validate questions. */
    public QuestionService(QuestionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** Select a random question of the requested level, unless one is already awaiting an answer. */
    public Question startQuestion(QuestionLevel level) {
        Objects.requireNonNull(level, "level");
        if (pendingQuestion != null) {
            throw new IllegalStateException("A question is already awaiting an answer");
        }
        Question question = repository.findRandom(level)
                .orElseThrow(() -> new IllegalStateException("No questions available for level: " + level));
        pendingQuestion = question;
        return question;
    }

    /** Return the question awaiting an answer, if there is one. */
    public Optional<Question> pendingQuestion() {
        return Optional.ofNullable(pendingQuestion);
    }

    /** Check the chosen 0-based option exactly once and return the result without revealing the answer key. */
    public AnswerResult submitAnswer(int selectedIndex) {
        if (pendingQuestion == null) {
            throw new IllegalStateException("No question is awaiting an answer");
        }
        Question question = pendingQuestion;
        // The repository validates the index and owns the protected correct answer.
        boolean correct = repository.isCorrect(question.id(), selectedIndex);
        pendingQuestion = null; // Only a valid submission consumes the question.
        return new AnswerResult(question.id(), question.level(), correct);
    }

    /** Public answer outcome for the future game flow; contains no correct-answer index. */
    public record AnswerResult(int questionId, QuestionLevel level, boolean correct) {
        /** Validate the public result's immutable metadata. */
        public AnswerResult {
            if (questionId <= 0) throw new IllegalArgumentException("Question id must be positive");
            Objects.requireNonNull(level, "level");
        }
    }
}
