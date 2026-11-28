package Model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Defines the project's FR-37 question movement, using the original Ludo Project-V2 rules. */
public final class QuestionMovementRules {
    private final Map<QuestionLevel, Integer> correctAnswerSteps;

    /** Validate an explicit forward-step count for every question difficulty. */
    public QuestionMovementRules(Map<QuestionLevel, Integer> correctAnswerSteps) {
        Objects.requireNonNull(correctAnswerSteps, "correctAnswerSteps");
        EnumMap<QuestionLevel, Integer> copy = new EnumMap<>(QuestionLevel.class);
        for (QuestionLevel level : QuestionLevel.values()) {
            Integer steps = correctAnswerSteps.get(level);
            if (steps == null || steps <= 0) {
                throw new IllegalArgumentException("Missing or invalid forward steps for " + level);
            }
            copy.put(level, steps);
        }
        this.correctAnswerSteps = Map.copyOf(copy);
    }

    /** Original Ludo Project-V2, p. 4: correct +1/+2/+3, incorrect -1/-2/-3. */
    public static QuestionMovementRules projectRules() {
        EnumMap<QuestionLevel, Integer> steps = new EnumMap<>(QuestionLevel.class);
        for (QuestionLevel level : QuestionLevel.values()) {
            steps.put(level, level.wrongAnswerPenalty());
        }
        return new QuestionMovementRules(steps);
    }

    /** Return positive steps for a correct answer, negative steps for an incorrect answer (FR-37). */
    public int signedSteps(QuestionService.AnswerResult result) {
        Objects.requireNonNull(result, "result");
        return result.correct() ? correctAnswerSteps.get(result.level())
                : -result.level().wrongAnswerPenalty();
    }
}
