package Model;

/** The six surprise outcomes and their probabilities (FR-38 to FR-44). */
public enum SurpriseType {
    DIRECT_FINISH(10, "מעבר ישיר לבית הסיום"),
    MOVE_OPPONENT_BACKWARD(10, "הזזת כלי של יריב לאחור"),
    RETURN_TO_CASTLE(10, "חזרה לטירה"),
    MOVE_BACKWARD_THREE(30, "שלושה צעדים לאחור"),
    LOSE_NEXT_TURN(30, "הפסד התור הבא"),
    ULTRA_BOOST_ROLL(10, "הטלת UltraBoost מיוחדת");

    private final int probabilityPercent;
    private final String displayName;

    SurpriseType(int probabilityPercent, String displayName) {
        this.probabilityPercent = probabilityPercent;
        this.displayName = displayName;
    }

    /** Probability of drawing this outcome, expressed as a percentage. */
    public int probabilityPercent() {
        return probabilityPercent;
    }

    /** Hebrew description for player-facing feedback. */
    public String displayName() {
        return displayName;
    }
}