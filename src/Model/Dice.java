package Model;

import java.util.Objects;
import java.util.Random;

/**
 * The game's dice: the regular step die (1-6) and the multiplier dice Boost (1-3) and
 * UltraBoost (1-6), each with equal probability for every face (FR-15).
 * The random source is injected so unit tests can script the rolls.
 */
public class Dice {
    private final Random random;

    /** Initialize the random step die for play or deterministic tests. */
    public Dice() { this(new Random()); }

    /** Initialize the random step die for play or deterministic tests. */
    public Dice(Random random) { this.random = Objects.requireNonNull(random); }

    /** Roll the regular step die once. */
    public DiceRoll roll() { return new DiceRoll(1 + random.nextInt(GameRules.DIE_FACES)); }

    /** Roll a Boost or UltraBoost die once and return its face. */
    public int rollMultiplier(MultiplierDie die) {
        Objects.requireNonNull(die, "die");
        if (die == MultiplierDie.NONE) throw new IllegalArgumentException("No multiplier die to roll");
        return 1 + random.nextInt(die.faces());
    }
}
