package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FR-18, FR-19, FR-20, FR-46: turn rules in TurnManager. Owner: Gal (SDP 5.2.1).
 *
 * Decision table for afterRoll (inputs: roll, legal move, sixes before this roll):
 *   roll | legal move | sixes before | outcome
 *   6    | yes        | 0 or 1       | CHOOSE_PIECE
 *   6    | no         | 0 or 1       | ROLL_AGAIN
 *   1-5  | yes        | any          | CHOOSE_PIECE (streak resets)
 *   1-5  | no         | any          | PASS_TURN (streak resets)
 *   6    | yes or no  | 2            | THIRD_SIX_CANCELLED
 */
class TurnManagerTest {

    private static final DiceRoll SIX = new DiceRoll(6);
    private static final DiceRoll THREE = new DiceRoll(3);

    // ---------- afterRoll: one test per row of the decision table ----------

    @Test
    @DisplayName("FR-19: a six with a legal move lets the player choose a piece")
    void sixWithLegalMoveChoosesPiece() {
        GameState state = TestSupport.newState(2);
        assertEquals(TurnManager.RollOutcome.CHOOSE_PIECE, TurnManager.afterRoll(state, SIX, true));
        assertEquals(1, state.consecutiveSixes());
    }

    @Test
    @DisplayName("FR-19: a six with no legal move gives another roll")
    void sixWithoutLegalMoveRollsAgain() {
        GameState state = TestSupport.newState(2);
        assertEquals(TurnManager.RollOutcome.ROLL_AGAIN, TurnManager.afterRoll(state, SIX, false));
    }

    @Test
    @DisplayName("FR-18: a non-six with a legal move lets the player choose a piece")
    void nonSixWithLegalMoveChoosesPiece() {
        GameState state = TestSupport.newState(2);
        assertEquals(TurnManager.RollOutcome.CHOOSE_PIECE, TurnManager.afterRoll(state, THREE, true));
    }

    @Test
    @DisplayName("FR-18: a non-six with no legal move passes the turn")
    void nonSixWithoutLegalMovePassesTurn() {
        GameState state = TestSupport.newState(2);
        assertEquals(TurnManager.RollOutcome.PASS_TURN, TurnManager.afterRoll(state, THREE, false));
    }

    @Test
    @DisplayName("FR-20: the third six in a row is cancelled even when a move exists")
    void thirdSixIsCancelledWithLegalMove() {
        GameState state = TestSupport.newState(2);
        TurnManager.afterRoll(state, SIX, true);
        TurnManager.afterRoll(state, SIX, true);
        assertEquals(TurnManager.RollOutcome.THIRD_SIX_CANCELLED, TurnManager.afterRoll(state, SIX, true));
    }

    @Test
    @DisplayName("FR-20: the third six in a row is cancelled when no move exists")
    void thirdSixIsCancelledWithoutLegalMove() {
        GameState state = TestSupport.newState(2);
        TurnManager.afterRoll(state, SIX, false);
        TurnManager.afterRoll(state, SIX, false);
        assertEquals(TurnManager.RollOutcome.THIRD_SIX_CANCELLED, TurnManager.afterRoll(state, SIX, false));
    }

    @Test
    @DisplayName("FR-20 boundary: the second six is not cancelled")
    void secondSixIsNotCancelled() {
        GameState state = TestSupport.newState(2);
        TurnManager.afterRoll(state, SIX, true);
        assertEquals(TurnManager.RollOutcome.CHOOSE_PIECE, TurnManager.afterRoll(state, SIX, true));
        assertEquals(2, state.consecutiveSixes());
    }

    @Test
    @DisplayName("FR-20: a non-six breaks the streak, so 6,6,3,6 is not a third six")
    void nonSixResetsTheStreak() {
        GameState state = TestSupport.newState(2);
        TurnManager.afterRoll(state, SIX, true);
        TurnManager.afterRoll(state, SIX, true);
        TurnManager.afterRoll(state, THREE, true);
        assertEquals(0, state.consecutiveSixes());
        assertEquals(TurnManager.RollOutcome.CHOOSE_PIECE, TurnManager.afterRoll(state, SIX, true));
    }

    // ---------- rollsAgain ----------

    @Test
    @DisplayName("FR-19: after moving with a six, the player rolls again")
    void sixAfterMoveRollsAgain() {
        Player player = TestSupport.newState(2).players().get(0);
        assertTrue(TurnManager.rollsAgain(player, SIX));
        assertFalse(TurnManager.rollsAgain(player, THREE));
    }

    @Test
    @DisplayName("FR-46: a player who has just finished does not roll again after a six")
    void finishedPlayerDoesNotRollAgain() {
        Player player = TestSupport.newState(2).players().get(0);
        player.assignRank(1, true);
        assertFalse(TurnManager.rollsAgain(player, SIX));
    }

    // ---------- passTurn ----------

    @Test
    @DisplayName("FR-18: the turn passes to the next player in order")
    void turnPassesToNextPlayer() {
        GameState state = TestSupport.newState(3);
        TurnManager.passTurn(state);
        assertEquals(1, state.currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-18: after the last player the turn goes back to the first")
    void turnWrapsAroundToFirstPlayer() {
        GameState state = TestSupport.newState(3);
        state.setCurrentPlayerIndex(2);
        TurnManager.passTurn(state);
        assertEquals(0, state.currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-46: a player who finished is skipped")
    void finishedPlayerIsSkipped() {
        GameState state = TestSupport.newState(4);
        state.players().get(1).assignRank(1, true);
        TurnManager.passTurn(state);
        assertEquals(2, state.currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-46: several finished players in a row are all skipped")
    void severalFinishedPlayersAreSkipped() {
        GameState state = TestSupport.newState(4);
        state.players().get(1).assignRank(1, true);
        state.players().get(2).assignRank(2, true);
        TurnManager.passTurn(state);
        assertEquals(3, state.currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-18, FR-20: passing the turn resets the six streak and waits for a new roll")
    void passTurnResetsTurnState() {
        GameState state = TestSupport.newState(2);
        state.setConsecutiveSixes(2);
        state.setPhase(GameState.Phase.WAITING_FOR_MOVE);
        state.setLegalMoves(List.of(new Move(0, -1, 0, true, false)));
        state.setLastRoll(SIX);

        TurnManager.passTurn(state);

        assertEquals(0, state.consecutiveSixes());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
        assertTrue(state.legalMoves().isEmpty());
        assertNull(state.lastRoll(), "No active roll for the next player");
        assertEquals(SIX, state.latestRoll(), "The die keeps showing the last result");
    }

    @Test
    @DisplayName("FR-19 decision table: a second six with no legal move still gives another roll")
    void secondSixWithoutLegalMoveRollsAgain() {
        GameState state = TestSupport.newState(2);
        TurnManager.afterRoll(state, SIX, false);
        assertEquals(TurnManager.RollOutcome.ROLL_AGAIN, TurnManager.afterRoll(state, SIX, false));
        assertEquals(2, state.consecutiveSixes());
    }

    @Test
    @DisplayName("FR-18, FR-20: after two sixes, a non-six with no legal move passes the turn and resets the streak")
    void nonSixAfterTwoSixesWithoutMovePassesTurn() {
        GameState state = TestSupport.newState(2);
        TurnManager.afterRoll(state, SIX, true);
        TurnManager.afterRoll(state, SIX, true);
        assertEquals(TurnManager.RollOutcome.PASS_TURN, TurnManager.afterRoll(state, THREE, false));
        assertEquals(0, state.consecutiveSixes());
    }

    @Test
    @DisplayName("FR-46: a finished last player is skipped when the turn wraps around to the first")
    void finishedPlayerIsSkippedAcrossWrapAround() {
        GameState state = TestSupport.newState(4);
        state.players().get(3).assignRank(1, true);
        state.setCurrentPlayerIndex(2);
        TurnManager.passTurn(state);
        assertEquals(0, state.currentPlayerIndex());
    }
}
