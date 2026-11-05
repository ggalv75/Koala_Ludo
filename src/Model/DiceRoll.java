package Model;

/** Result of one roll of the regular step die (FR-15). */
public record DiceRoll(int value) {
    public DiceRoll {
        if (value < 1 || value > GameRules.DIE_FACES) {
            throw new IllegalArgumentException("Die value must be 1-" + GameRules.DIE_FACES);
        }
    }

    public boolean isSix() { return value == GameRules.SIX; }
}
