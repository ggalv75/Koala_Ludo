package Model;

import java.util.Objects;

/**
 * Result of one throw: the regular step die and, at Medium/Hard or in the UltraBoost surprise,
 * the multiplier die (FR-15). {@link #value()} is always the step die, so the rule of six
 * (FR-19, FR-20) and releasing a piece (FR-24) depend on the step die only.
 *
 * Step calculation (FR-16, FR-17): with a multiplier value N, a piece on the shared track moves
 * {@code value x (N - 1)} squares, which can be 0. A piece already on its home path moves by
 * {@code value} only.
 */
public record DiceRoll(int value, MultiplierDie multiplierDie, int multiplierValue) {

    /** Reject die values that cannot come up on the dice that were rolled. */
    public DiceRoll {
        if (value < 1 || value > GameRules.DIE_FACES) {
            throw new IllegalArgumentException("Die value must be 1-" + GameRules.DIE_FACES);
        }
        Objects.requireNonNull(multiplierDie, "multiplierDie");
        if (!multiplierDie.accepts(multiplierValue)) {
            throw new IllegalArgumentException(multiplierDie + " value out of range: " + multiplierValue);
        }
    }

    /** A throw of the regular step die only (Easy level). */
    public DiceRoll(int value) { this(value, MultiplierDie.NONE, 0); }

    /** The same step die result together with a multiplier die result. */
    public DiceRoll withMultiplier(MultiplierDie die, int dieValue) {
        return new DiceRoll(value, die, dieValue);
    }

    /** Report whether the step die shows six (extra roll, release from the castle). */
    public boolean isSix() { return value == GameRules.SIX; }

    /** True when a Boost or UltraBoost die was rolled with the step die. */
    public boolean hasMultiplier() { return multiplierDie != MultiplierDie.NONE; }

    /** FR-16: the coefficient N - 1 (0 to 5); 1 when no multiplier die was rolled. */
    public int factor() { return hasMultiplier() ? multiplierValue - 1 : 1; }

    /** FR-16: squares a piece on the shared track moves (may be 0). */
    public int trackSteps() { return value * factor(); }

    /** FR-17: squares a piece on its home path moves, without the multiplier. */
    public int homePathSteps() { return value; }
}
