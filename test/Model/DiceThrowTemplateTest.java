package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Template Method: DiceThrow.perform() fixes the order of a throw; the subclasses only change
 * the multiplier step and the six-streak hook (FR-15, FR-16, FR-19 to FR-21). Owner: Gal (iteration 2).
 */
class DiceThrowTemplateTest {

    @Test
    @DisplayName("Template Method: perform() is final, so no subclass can change the order of the steps")
    void templateMethodIsFinal() throws NoSuchMethodException {
        Method perform = DiceThrow.class.getDeclaredMethod("perform", GameState.class);
        assertTrue(Modifier.isFinal(perform.getModifiers()));
        assertTrue(Modifier.isAbstract(DiceThrow.class.getModifiers()));
    }

    @Test
    @DisplayName("NFR-05: the level's multiplier die chooses the regular throw")
    void regularThrowFollowsTheLevel() {
        Dice dice = new Dice();
        assertInstanceOf(StepDieThrow.class, DiceThrow.regularFor(MultiplierDie.NONE, dice));
        MultiplierDieThrow boost = assertInstanceOf(MultiplierDieThrow.class,
                DiceThrow.regularFor(MultiplierDie.BOOST, dice));
        assertEquals(MultiplierDie.BOOST, boost.multiplierDie());
        MultiplierDieThrow ultra = assertInstanceOf(MultiplierDieThrow.class,
                DiceThrow.regularFor(MultiplierDie.ULTRA_BOOST, dice));
        assertEquals(MultiplierDie.ULTRA_BOOST, ultra.multiplierDie());
    }

    @Test
    @DisplayName("FR-15: the Easy throw rolls the step die only (a second roll would fail the script)")
    void easyThrowRollsOnlyTheStepDie() {
        GameState state = TestSupport.newState(2);
        ThrowResult result = new StepDieThrow(scripted(4)).perform(state);
        assertEquals(new DiceRoll(4), result.roll());
        assertEquals(TurnManager.RollOutcome.PASS_TURN, result.outcome(), "All pieces in the castle");
    }

    @Test
    @DisplayName("FR-15: a Boost throw rolls the step die first, then the Boost die")
    void boostThrowRollsStepDieThenBoost() {
        GameState state = TestSupport.newState(2);
        ThrowResult result = new MultiplierDieThrow(scripted(6, 3), MultiplierDie.BOOST).perform(state);
        assertEquals(new DiceRoll(6, MultiplierDie.BOOST, 3), result.roll());
    }

    @Test
    @DisplayName("Template step 3: the legal moves come from MoveValidator with the multiplied count")
    void legalMovesUseTheMultipliedCount() {
        GameState state = TestSupport.newState(2);
        Piece piece = state.piecesOf(state.currentPlayer()).get(0);
        piece.moveTo(10);
        ThrowResult result = new MultiplierDieThrow(scripted(4, 3), MultiplierDie.BOOST).perform(state);
        assertEquals(TurnManager.RollOutcome.CHOOSE_PIECE, result.outcome());
        assertEquals(1, result.legalMoves().size());
        assertEquals(18, result.legalMoves().get(0).toStep());
    }

    @Test
    @DisplayName("FR-20: a regular throw counts toward the six streak; the third six is cancelled")
    void regularThrowCountsTowardTheStreak() {
        GameState state = TestSupport.newState(2);
        state.setConsecutiveSixes(2);
        ThrowResult result = new MultiplierDieThrow(scripted(6, 2), MultiplierDie.BOOST).perform(state);
        assertEquals(TurnManager.RollOutcome.THIRD_SIX_CANCELLED, result.outcome());
        assertEquals(3, state.consecutiveSixes());
    }

    @Test
    @DisplayName("FR-21: the surprise throw is UltraBoost and is not cancelled after two sixes")
    void surpriseThrowIsNotCancelled() {
        GameState state = TestSupport.newState(2);
        state.setConsecutiveSixes(2);
        ThrowResult result = new SurpriseUltraBoostThrow(scripted(6, 5)).perform(state);
        assertEquals(new DiceRoll(6, MultiplierDie.ULTRA_BOOST, 5), result.roll());
        assertEquals(TurnManager.RollOutcome.CHOOSE_PIECE, result.outcome(), "A six releases a piece");
        assertEquals(2, state.consecutiveSixes(), "Not counted in the streak");
    }

    @Test
    @DisplayName("FR-21: a non-six surprise throw does not reset the streak")
    void surpriseThrowDoesNotResetTheStreak() {
        GameState state = TestSupport.newState(2);
        state.setConsecutiveSixes(2);
        new SurpriseUltraBoostThrow(scripted(3, 4)).perform(state);
        assertEquals(2, state.consecutiveSixes());
    }

    @Test
    @DisplayName("FR-19, FR-21: a six in the surprise throw with no legal move gives another roll")
    void surpriseSixWithoutMoveRollsAgain() {
        GameState state = TestSupport.newState(2);
        // Block the release: an opponent Double Block on Gal's start square (FR-30).
        Player dor = state.players().get(1);
        int startForDor = stepOnRouteOf(dor, startSquareOf(state.currentPlayer(), state), state);
        state.piecesOf(dor).get(0).moveTo(startForDor);
        state.piecesOf(dor).get(1).moveTo(startForDor);
        ThrowResult result = new SurpriseUltraBoostThrow(scripted(6, 2)).perform(state);
        assertEquals(TurnManager.RollOutcome.ROLL_AGAIN, result.outcome());
    }

    @Test
    @DisplayName("Log text: Easy keeps the iteration 1 text, multiplier throws show the die and coefficient")
    void describeTexts() {
        Dice dice = new Dice();
        assertEquals("תוצאת הקובייה - 4", new StepDieThrow(dice).describe(new DiceRoll(4)));
        assertEquals("קובייה 4, Boost 3 (מקדם 2)",
                new MultiplierDieThrow(dice, MultiplierDie.BOOST).describe(new DiceRoll(4, MultiplierDie.BOOST, 3)));
        assertTrue(new SurpriseUltraBoostThrow(dice)
                .describe(new DiceRoll(2, MultiplierDie.ULTRA_BOOST, 6)).startsWith("הטלה מיוחדת: "));
    }

    @Test
    @DisplayName("A multiplier throw needs a real multiplier die")
    void multiplierThrowRejectsNone() {
        assertThrows(IllegalArgumentException.class, () -> new MultiplierDieThrow(new Dice(), MultiplierDie.NONE));
    }

    private static Dice scripted(Integer... values) {
        return new Dice(new TestSupport.ScriptedRandom(values));
    }

    private static Position startSquareOf(Player player, GameState state) {
        return state.board().positionOf(player.color(), 0);
    }

    /** The step on the opponent's route that is the given square. */
    private static int stepOnRouteOf(Player opponent, Position square, GameState state) {
        for (int step = 0; step < state.board().routeLength(); step++) {
            if (square.equals(state.board().positionOf(opponent.color(), step))) return step;
        }
        throw new IllegalStateException(square + " is not on " + opponent.color().name() + "'s route");
    }
}
