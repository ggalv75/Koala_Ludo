package Model;

import java.util.Objects;

/**
 * Template Method, concrete class: a throw of the step die together with a multiplier die
 * (FR-15, FR-16). The regular throw at Medium (Boost) and Hard (UltraBoost).
 */
class MultiplierDieThrow extends DiceThrow {
    private final MultiplierDie multiplierDie;

    /** Throw that rolls the given Boost or UltraBoost die next to the step die. */
    MultiplierDieThrow(Dice dice, MultiplierDie multiplierDie) {
        super(dice);
        this.multiplierDie = Objects.requireNonNull(multiplierDie, "multiplierDie");
        if (multiplierDie == MultiplierDie.NONE) {
            throw new IllegalArgumentException("Use StepDieThrow for a throw without a multiplier die");
        }
    }

    /** Step 2: roll the multiplier die and attach its value to the step die result. */
    @Override
    protected DiceRoll addMultiplier(DiceRoll stepRoll) {
        return stepRoll.withMultiplier(multiplierDie, dice().rollMultiplier(multiplierDie));
    }

    /** The multiplier die this throw rolls. */
    MultiplierDie multiplierDie() { return multiplierDie; }
}
