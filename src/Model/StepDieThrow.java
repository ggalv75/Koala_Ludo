package Model;

/** Template Method, concrete class: the Easy-level throw, regular step die only (FR-15). */
final class StepDieThrow extends DiceThrow {

    /** Throw that uses only the game's step die. */
    StepDieThrow(Dice dice) { super(dice); }

    /** Step 2: Easy has no multiplier die, so the step die result is the whole throw. */
    @Override
    protected DiceRoll addMultiplier(DiceRoll stepRoll) { return stepRoll; }
}
