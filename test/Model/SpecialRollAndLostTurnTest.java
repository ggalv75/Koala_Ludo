package Model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FR-21, FR-22 (with FR-19, FR-20, FR-46): turn management for the two surprises that change
 * turns. The surprise mechanism itself is not here yet, so each test calls
 * {@link TurnManager#grantSpecialRoll} / {@link TurnManager#loseNextTurn} at the moment the
 * surprise would fire: after the roll and before the move is completed.
 * The die is scripted; with UltraBoost the script holds the step die, then the UltraBoost die.
 * Owner: Gal (iteration 2).
 *
 * Decision table for TurnManager.afterMove:
 *   finished | lost turn | special pending | step die 6 | owed roll | result
 *   yes      | -         | -               | -          | -         | PASS_TURN   (FR-46)
 *   no       | yes       | -               | any        | -         | PASS_TURN   (FR-22)
 *   no       | no        | yes             | yes        | -         | SPECIAL_ROLL, extra roll owed
 *   no       | no        | yes             | no         | -         | SPECIAL_ROLL
 *   no       | no        | no              | yes        | -         | ROLL_AGAIN  (FR-19)
 *   no       | no        | no              | no         | yes       | ROLL_AGAIN, owed roll used
 *   no       | no        | no              | no         | no        | PASS_TURN   (FR-18)
 */
class SpecialRollAndLostTurnTest {

    private static final int GAL_FIRST_PIECE = 0;
    private static final int GAL = 0;
    private static final int DOR = 1;
    private static final int OMER = 2;

    // ---------- FR-21: the UltraBoost surprise throw through the engine ----------

    @Test
    @DisplayName("FR-21: at Easy the surprise throw rolls the step die and UltraBoost")
    void surpriseThrowRollsUltraBoostAtEasy() {
        GameEngine engine = TestSupport.newEngine(2, 6, 2);
        TurnManager.grantSpecialRoll(engine.state());

        engine.rollDice();

        GameState state = engine.state();
        assertEquals(new DiceRoll(6, MultiplierDie.ULTRA_BOOST, 2), state.lastRoll());
        assertFalse(state.specialRollPending(), "The special throw is used once");
        assertEquals(0, state.consecutiveSixes(), "Not counted in the six streak");
        assertEquals(GameState.Phase.WAITING_FOR_MOVE, state.phase());
    }

    @Test
    @DisplayName("FR-21: the surprise throw comes right after the move that triggered it")
    void surpriseThrowComesRightAfterTheMove() {
        GameEngine engine = TestSupport.newEngine(2, 6, 6, 3, 4);
        GameState state = engine.state();
        engine.rollDice();
        engine.movePiece(GAL_FIRST_PIECE);          // release with the first six
        engine.rollDice();                          // 6: second six in a row
        TurnManager.grantSpecialRoll(state);        // the move lands on an UltraBoost surprise
        engine.movePiece(GAL_FIRST_PIECE);

        assertTrue(state.specialRollPending());
        assertEquals(GAL, state.currentPlayerIndex());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());

        engine.rollDice();                          // special throw: 3 x (4 - 1) = 9 squares
        assertEquals(MultiplierDie.ULTRA_BOOST, state.lastRoll().multiplierDie());
        engine.movePiece(GAL_FIRST_PIECE);
        assertEquals(6 + 9, state.piece(GAL_FIRST_PIECE).step());
    }

    @Test
    @DisplayName("FR-21, FR-20: after two sixes the surprise throw's six is not cancelled")
    void surpriseSixAfterTwoSixesIsNotCancelled() {
        GameEngine engine = TestSupport.newEngine(2, 6, 6, 6, 2, 6);
        GameState state = engine.state();
        engine.rollDice();
        engine.movePiece(GAL_FIRST_PIECE);          // six #1
        engine.rollDice();                          // six #2
        TurnManager.grantSpecialRoll(state);
        engine.movePiece(GAL_FIRST_PIECE);

        engine.rollDice();                          // special throw: 6 with UltraBoost 2
        assertEquals(GameState.Phase.WAITING_FOR_MOVE, state.phase(), "Not cancelled");
        assertEquals(2, state.consecutiveSixes(), "Still two sixes");

        engine.movePiece(GAL_FIRST_PIECE);          // the special six earns another roll
        assertEquals(GAL, state.currentPlayerIndex());
        engine.rollDice();                          // regular six #3 is cancelled
        assertEquals(DOR, state.currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-19, FR-21: the extra roll from the six that reached the surprise comes after the special throw")
    void sixBeforeTheSurpriseKeepsItsExtraRoll() {
        GameEngine engine = TestSupport.newEngine(2, 6, 2, 2, 3);
        GameState state = engine.state();
        engine.rollDice();                          // 6
        TurnManager.grantSpecialRoll(state);
        engine.movePiece(GAL_FIRST_PIECE);          // release, lands on the surprise

        engine.rollDice();                          // special: 2 x (2 - 1) = 2
        engine.movePiece(GAL_FIRST_PIECE);
        assertEquals(GAL, state.currentPlayerIndex(), "The six's extra roll is kept");
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());

        engine.rollDice();                          // 3
        engine.movePiece(GAL_FIRST_PIECE);
        assertEquals(DOR, state.currentPlayerIndex(), "The kept roll is used once");
    }

    @Test
    @DisplayName("FR-18, FR-21: a surprise throw with no move and no six passes the turn")
    void surpriseThrowWithoutMovePassesTurn() {
        GameEngine engine = TestSupport.newEngine(2, 3, 5);
        TurnManager.grantSpecialRoll(engine.state());

        engine.rollDice();

        GameState state = engine.state();
        assertEquals(DOR, state.currentPlayerIndex());
        assertNull(state.lastRoll());
        assertEquals(MultiplierDie.ULTRA_BOOST, state.latestRoll().multiplierDie(), "The die keeps showing it");
    }

    @Test
    @DisplayName("FR-21: a pending surprise throw does not pass to the next player")
    void pendingSurpriseThrowIsClearedWhenTheTurnPasses() {
        GameState state = TestSupport.newState(2);
        TurnManager.grantSpecialRoll(state);
        TurnManager.passTurn(state);
        assertFalse(state.specialRollPending());
    }

    // ---------- FR-22: losing the next turn ----------

    @Test
    @DisplayName("FR-22: losing a turn cancels the extra roll of a six and skips the next turn")
    void lostTurnCancelsExtraRollAndSkipsNextTurn() {
        GameEngine engine = TestSupport.newEngine(2, 6, 3, 2);
        GameState state = engine.state();
        Player gal = state.players().get(GAL);
        engine.rollDice();                          // 6
        TurnManager.loseNextTurn(state, gal);       // the move lands on a lose-turn surprise
        assertTrue(state.losesNextTurn(gal));
        engine.movePiece(GAL_FIRST_PIECE);
        assertEquals(DOR, state.currentPlayerIndex(), "No extra roll for the six");

        engine.rollDice();                          // Dor: 3, no move
        assertEquals(DOR, state.currentPlayerIndex(), "Gal's turn is skipped");
        assertFalse(state.losesNextTurn(gal), "The penalty is used up");

        engine.rollDice();                          // Dor: 2, no move
        assertEquals(GAL, state.currentPlayerIndex(), "Gal plays again");
    }

    @Test
    @DisplayName("FR-22: with three players the order is Gal, Dor, Omer, (Gal skipped), Dor")
    void lostTurnWithThreePlayers() {
        GameState state = TestSupport.newState(3);
        TurnManager.loseNextTurn(state, state.players().get(GAL));
        TurnManager.passTurn(state);
        assertEquals(DOR, state.currentPlayerIndex());
        TurnManager.passTurn(state);
        assertEquals(OMER, state.currentPlayerIndex());
        TurnManager.passTurn(state);
        assertEquals(DOR, state.currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-22: two players who both lose a turn are each skipped once")
    void twoPenalisedPlayersAreEachSkippedOnce() {
        GameState state = TestSupport.newState(2);
        TurnManager.loseNextTurn(state, state.players().get(DOR));
        TurnManager.loseNextTurn(state, state.players().get(GAL));
        TurnManager.passTurn(state);                // Dor skipped, Gal skipped
        assertEquals(DOR, state.currentPlayerIndex());
        assertFalse(state.losesNextTurn(state.players().get(GAL)));
        assertFalse(state.losesNextTurn(state.players().get(DOR)));
    }

    @Test
    @DisplayName("FR-22, FR-46: finished and penalised players are skipped together")
    void finishedAndPenalisedPlayersAreSkipped() {
        GameState state = TestSupport.newState(3);
        state.players().get(OMER).assignRank(1, true);
        TurnManager.loseNextTurn(state, state.players().get(DOR));
        TurnManager.passTurn(state);
        assertEquals(GAL, state.currentPlayerIndex());
    }

    // ---------- TurnManager.afterMove: one test per row of the decision table ----------

    @Test
    @DisplayName("FR-46: a finished player gets no surprise throw and no extra roll")
    void finishedPlayerPasses() {
        GameState state = TestSupport.newState(2);
        Player gal = state.players().get(GAL);
        gal.assignRank(1, true);
        TurnManager.grantSpecialRoll(state);
        assertEquals(TurnManager.AfterMove.PASS_TURN, TurnManager.afterMove(state, gal, new DiceRoll(6)));
    }

    @Test
    @DisplayName("FR-22: a player who lost the turn gets no extra roll even after a six")
    void forfeitedTurnPasses() {
        GameState state = TestSupport.newState(2);
        Player gal = state.players().get(GAL);
        TurnManager.loseNextTurn(state, gal);
        assertEquals(TurnManager.AfterMove.PASS_TURN, TurnManager.afterMove(state, gal, new DiceRoll(6)));
    }

    @Test
    @DisplayName("FR-21: a pending surprise throw comes next; a six on the move is owed for later")
    void specialRollWithSixOwesExtraRoll() {
        GameState state = TestSupport.newState(2);
        Player gal = state.players().get(GAL);
        TurnManager.grantSpecialRoll(state);
        assertEquals(TurnManager.AfterMove.SPECIAL_ROLL, TurnManager.afterMove(state, gal, new DiceRoll(6)));
        state.turnState().takeSpecialRoll();
        assertEquals(TurnManager.AfterMove.ROLL_AGAIN, TurnManager.afterMove(state, gal, new DiceRoll(2)),
                "The owed roll after the special throw");
    }

    @Test
    @DisplayName("FR-21: a pending surprise throw comes next after a non-six move")
    void specialRollWithoutSix() {
        GameState state = TestSupport.newState(2);
        Player gal = state.players().get(GAL);
        TurnManager.grantSpecialRoll(state);
        assertEquals(TurnManager.AfterMove.SPECIAL_ROLL, TurnManager.afterMove(state, gal, new DiceRoll(4)));
        state.turnState().takeSpecialRoll();
        assertEquals(TurnManager.AfterMove.PASS_TURN, TurnManager.afterMove(state, gal, new DiceRoll(4)),
                "Nothing owed");
    }

    @Test
    @DisplayName("FR-19, FR-18: without surprises a six rolls again and anything else passes")
    void plainSixAndPlainPass() {
        GameState state = TestSupport.newState(2);
        Player gal = state.players().get(GAL);
        assertEquals(TurnManager.AfterMove.ROLL_AGAIN, TurnManager.afterMove(state, gal, new DiceRoll(6)));
        assertEquals(TurnManager.AfterMove.PASS_TURN, TurnManager.afterMove(state, gal, new DiceRoll(5)));
    }
}
