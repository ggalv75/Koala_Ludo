package Model;

import java.util.List;
import java.util.Objects;

/** Immutable public question data; the correct answer is never exposed to the View. */
public record Question(int id, String topic, String text, List<String> options, QuestionLevel level) {

    /** Validate and defensively copy the four answer options (FR-35). */
    public Question {
        if (id <= 0) throw new IllegalArgumentException("Question id must be positive");
        topic = Objects.requireNonNull(topic, "topic").strip();
        text = Objects.requireNonNull(text, "text").strip();
        level = Objects.requireNonNull(level, "level");
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        if (topic.isBlank() || text.isBlank()) {
            throw new IllegalArgumentException("Question topic and text must not be blank");
        }
        if (options.size() != 4 || options.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("A question must have four nonblank options");
        }
    }
}
