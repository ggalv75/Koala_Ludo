package Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Turns and the rule of six, FR-18 to FR-20, FR-27 feedback (SDP 5.2.1: Gal). */
class TurnFlowTest {

    @Test
    @DisplayName("FR-18: no legal move and no six - the turn passes to the next player")
    void noMovePassesTurn() {
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(3));
        engine.rollDice();
        GameState state = engine.state();
        assertEquals(1, state.currentPlayerIndex());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
        assertNull(state.lastRoll());
    }

    @Test
    @DisplayName("FR-18 / FR-19: a six with no legal move still gives another roll")
    void sixWithoutMoveRollsAgain() {
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(6));
        GameState state = engine.state();
        Board board = state.board();
        int redStartForGreen = TestSupport.sameSquare(board, PlayerColor.GREEN, PlayerColor.RED, 0);
        TestSupport.piece(state, 1, 1).moveTo(redStartForGreen);
        TestSupport.piece(state, 1, 2).moveTo(redStartForGreen); // green Double Block on red's start

        engine.rollDice();

        assertEquals(0, state.currentPlayerIndex());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
        assertEquals(1, state.consecutiveSixes());
    }

    @Test
    @DisplayName("FR-19: after moving with a six the same player rolls again")
    void sixGivesExtraRoll() {
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(6));
        GameState state = engine.state();
        engine.rollDice();
        assertEquals(4, state.legalMoves().size(), "All four pieces may leave the castle");
        assertTrue(engine.movePiece(TestSupport.piece(state, 0, 1).id()));
        assertEquals(0, TestSupport.piece(state, 0, 1).step());
        assertEquals(0, state.currentPlayerIndex());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
    }

    @Test
    @DisplayName("FR-20: the third six in a row is cancelled and the turn passes without moving")
    void thirdSixCancelled() {
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(6, 6, 6));
        GameState state = engine.state();
        Piece first = TestSupport.piece(state, 0, 1);
        engine.rollDice();
        engine.movePiece(first.id());
        engine.rollDice();
        engine.movePiece(first.id());
        assertEquals(6, first.step());

        engine.rollDice();

        assertEquals(6, first.step(), "The cancelled roll moves nothing");
        assertEquals(1, state.currentPlayerIndex());
        assertEquals(0, state.consecutiveSixes());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
    }

    @Test
    @DisplayName("FR-20: any other value resets the six counter")
    void sixCounterResets() {
        GameState state = TestSupport.state(2, 1);
        assertFalse(TurnManager.registerRoll(state, new DiceRoll(6)));
        assertFalse(TurnManager.registerRoll(state, new DiceRoll(6)));
        assertFalse(TurnManager.registerRoll(state, new DiceRoll(4)));
        assertEquals(0, state.consecutiveSixes());
        assertFalse(TurnManager.registerRoll(state, new DiceRoll(6)));
        assertFalse(TurnManager.registerRoll(state, new DiceRoll(6)));
        assertTrue(TurnManager.registerRoll(state, new DiceRoll(6)));
    }

    @Test
    @DisplayName("FR-46: players who finished are skipped in the turn order")
    void finishedPlayersAreSkipped() {
        GameState state = TestSupport.state(3, 1);
        state.players().get(1).assignRank(1, true);
        TurnManager.passTurn(state);
        assertEquals(2, state.currentPlayerIndex());
        TurnManager.passTurn(state);
        assertEquals(0, state.currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-23: a player who has a legal move cannot roll again before moving")
    void cannotRollBeforeMoving() {
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(6));
        engine.rollDice();
        engine.rollDice(); // ignored - the scripted die would fail if it were rolled again
        assertEquals(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase());
    }

    @Test
    @DisplayName("FR-27 / NFR-01: choosing a piece that cannot move only gives feedback")
    void illegalPieceChoiceIsRejectedSafely() {
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(6));
        GameState state = engine.state();
        assertFalse(engine.movePiece(0), "Before rolling");
        engine.rollDice();
        assertFalse(engine.movePiece(TestSupport.piece(state, 1, 1).id()), "Opponent's piece");
        assertFalse(engine.movePiece(999), "Unknown piece");
        assertEquals(GameState.Phase.WAITING_FOR_MOVE, state.phase());
        assertEquals(0, state.currentPlayerIndex());
    }
}
