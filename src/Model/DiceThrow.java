package Model;

import java.util.List;
import java.util.Objects;

/**
 * <b>Template Method design pattern</b> (Practice6, "The Template Pattern"): one throw of the dice.
 *
 * <p>Every throw in the game runs the same algorithm, defined once in the final method
 * {@link #perform(GameState)}:
 * <ol>
 *   <li>roll the regular step die (FR-15) - the same in every throw;</li>
 *   <li>add the multiplier die - <i>primitive operation</i>, each subclass decides:
 *       none at Easy, Boost at Medium, UltraBoost at Hard and in the surprise;</li>
 *   <li>find the legal moves, with steps = die x (N - 1) on the track and the die only on the
 *       home path (FR-16, FR-17) - the same in every throw;</li>
 *   <li>apply the rule of six (FR-19, FR-20) - the same rule, but the <i>hook</i>
 *       {@link #countsTowardSixStreak()} lets the UltraBoost surprise stay outside the streak (FR-21).</li>
 * </ol>
 *
 * <p>Why this pattern: without it the order of these steps would be repeated in a separate
 * method per level and per surprise, and a fix in one copy (for example in the rule of six)
 * could be missed in another. {@code perform} is final, so a subclass can change a step but
 * never the order or skip the rule of six. Adding the Hard level in iteration 3 needs no new
 * class: {@link #regularFor(MultiplierDie, Dice)} already returns an UltraBoost throw.
 */
abstract class DiceThrow {
    private final Dice dice;

    /** Every throw rolls with the game's dice, so tests can script all values in order. */
    protected DiceThrow(Dice dice) { this.dice = Objects.requireNonNull(dice, "dice"); }

    /** The regular throw of a level: step die only, or step die plus the level's multiplier die. */
    static DiceThrow regularFor(MultiplierDie die, Dice dice) {
        return die == MultiplierDie.NONE ? new StepDieThrow(dice) : new MultiplierDieThrow(dice, die);
    }

    /** The template method: the fixed order of a throw. Final, so no subclass can change it. */
    final ThrowResult perform(GameState state) {
        DiceRoll stepRoll = rollStepDie();
        DiceRoll roll = addMultiplier(stepRoll);
        List<Move> moves = findLegalMoves(state, roll);
        TurnManager.RollOutcome outcome = applySixRule(state, roll, !moves.isEmpty());
        return new ThrowResult(roll, moves, outcome);
    }

    /** Step 1 (common): the regular step die, 1-6. */
    private DiceRoll rollStepDie() { return dice.roll(); }

    /** Step 2 (primitive operation): return the throw with this throw's multiplier die, if any. */
    protected abstract DiceRoll addMultiplier(DiceRoll stepRoll);

    /** Step 3 (common): legal moves; MoveValidator applies FR-16 / FR-17 to every piece. */
    private List<Move> findLegalMoves(GameState state, DiceRoll roll) {
        return MoveValidator.legalMoves(state, roll);
    }

    /** Step 4 (common rule, controlled by the hook): what the player may do with this throw. */
    private TurnManager.RollOutcome applySixRule(GameState state, DiceRoll roll, boolean hasLegalMove) {
        return TurnManager.afterRoll(state, roll, hasLegalMove, countsTowardSixStreak());
    }

    /** Hook: regular throws count toward the three-sixes streak (FR-20). */
    protected boolean countsTowardSixStreak() { return true; }

    /** Hook: the log line for this throw, for example "תוצאת הקובייה - 4". */
    String describe(DiceRoll roll) {
        if (!roll.hasMultiplier()) return "תוצאת הקובייה - " + roll.value();
        return "קובייה " + roll.value() + ", " + roll.multiplierDie().displayName() + " " + roll.multiplierValue()
                + " (מקדם " + roll.factor() + ")";
    }

    /** Lets subclasses roll their multiplier die with the same random source. */
    protected final Dice dice() { return dice; }
}
