package Model;

/**
 * Template Method, concrete class: the special throw from the UltraBoost surprise (FR-21, FR-44).
 * Same steps as a regular UltraBoost throw, but at every level and outside the six streak:
 * it neither counts toward three sixes nor resets the streak, so it is never cancelled.
 * A six on its step die still earns another (regular) roll.
 */
final class SurpriseUltraBoostThrow extends MultiplierDieThrow {

    /** Special throw of the step die and the UltraBoost die. */
    SurpriseUltraBoostThrow(Dice dice) { super(dice, MultiplierDie.ULTRA_BOOST); }

    /** Hook override, FR-21: the special throw is not part of the six streak. */
    @Override
    protected boolean countsTowardSixStreak() { return false; }

    /** Hook override: the log says this was the surprise throw. */
    @Override
    String describe(DiceRoll roll) { return "הטלה מיוחדת: " + super.describe(roll); }
}
