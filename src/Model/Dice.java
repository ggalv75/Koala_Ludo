package Model;

import java.util.Objects;
import java.util.Random;

/**
 * The regular step die, values 1-6 with equal probability (FR-15).
 * The random source is injected so unit tests can script the rolls.
 */
public class Dice {
    private final Random random;

    public Dice() { this(new Random()); }

    public Dice(Random random) { this.random = Objects.requireNonNull(random); }

    public DiceRoll roll() { return new DiceRoll(1 + random.nextInt(GameRules.DIE_FACES)); }
}
