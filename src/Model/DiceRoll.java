package Model;

/** Result of one roll of the regular step die (FR-15). */
public record DiceRoll(int value) {
    /** Reject any die value outside the supported range. */
    public DiceRoll {
        if (value < 1 || value > GameRules.DIE_FACES) {
            throw new IllegalArgumentException("Die value must be 1-" + GameRules.DIE_FACES);
        }
    }

    /** Report whether the roll grants the normal extra turn. */
    public boolean isSix() { return value == GameRules.SIX; }
}
