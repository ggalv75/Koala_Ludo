package Model;

/** Difficulty of a question, independent of the selected game difficulty. */
public enum QuestionLevel {
    EASY(1),
    MEDIUM(2),
    HARD(3);

    private final int wrongAnswerPenalty;

    /** Store the number of steps lost for an incorrect answer (FR-37). */
    QuestionLevel(int wrongAnswerPenalty) {
        this.wrongAnswerPenalty = wrongAnswerPenalty;
    }

    /** Return the backward movement for an incorrect answer at this question level. */
    public int wrongAnswerPenalty() {
        return wrongAnswerPenalty;
    }
}
