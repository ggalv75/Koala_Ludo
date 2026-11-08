package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FR-15 to FR-20 through the GameEngine, the way the Controller calls it.
 * The die is scripted, so each test plays an exact sequence of rolls. Owner: Gal (SDP 5.2.1).
 */
class TurnFlowTest {

    private static final int GAL_FIRST_PIECE = 0;

    @Test
    @DisplayName("FR-18: all pieces in the castle and no six, so the turn passes")
    void noSixAtStartPassesTurn() {
        GameEngine engine = TestSupport.newEngine(2, 3);
        engine.rollDice();

        GameState state = engine.state();
        assertEquals(1, state.currentPlayerIndex());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
        assertNull(state.lastRoll(), "No active roll for the next player");
        assertEquals(3, state.latestRoll().value(), "The die still shows the roll after the turn passes");
    }

    @Test
    @DisplayName("FR-19: a six releases a piece and the same player rolls again")
    void sixReleasesPieceAndGivesAnotherRoll() {
        GameEngine engine = TestSupport.newEngine(2, 6);
        engine.rollDice();
        assertEquals(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase());

        assertTrue(engine.movePiece(GAL_FIRST_PIECE));

        GameState state = engine.state();
        assertEquals(0, state.piece(GAL_FIRST_PIECE).step(), "The piece is on its start square");
        assertEquals(0, state.currentPlayerIndex(), "Still Gal's turn");
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
    }

    @Test
    @DisplayName("FR-16, FR-18: a piece moves by the die value and then the turn passes")
    void pieceMovesByDieValueThenTurnPasses() {
        GameEngine engine = TestSupport.newEngine(2, 6, 4);
        engine.rollDice();
        engine.movePiece(GAL_FIRST_PIECE);
        engine.rollDice();
        engine.movePiece(GAL_FIRST_PIECE);

        assertEquals(4, engine.state().piece(GAL_FIRST_PIECE).step());
        assertEquals(1, engine.state().currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-20: on 6,6,6 the third roll is cancelled, the piece stays and the turn passes")
    void thirdSixInARowIsCancelled() {
        GameEngine engine = TestSupport.newEngine(2, 6, 6, 6);
        engine.rollDice();
        engine.movePiece(GAL_FIRST_PIECE); // release
        engine.rollDice();
        engine.movePiece(GAL_FIRST_PIECE); // 6 steps forward
        engine.rollDice();                 // third six

        GameState state = engine.state();
        assertEquals(6, state.piece(GAL_FIRST_PIECE).step(), "The third six moved nothing");
        assertEquals(1, state.currentPlayerIndex());
        assertEquals(0, state.consecutiveSixes());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
    }

    @Test
    @DisplayName("FR-46: a finished player is skipped when the turn passes")
    void finishedPlayerIsSkippedInEngine() {
        GameEngine engine = TestSupport.newEngine(3, 2);
        engine.state().players().get(1).assignRank(1, true);

        engine.rollDice();

        assertEquals(2, engine.state().currentPlayerIndex());
    }

    @Test
    @DisplayName("NFR-01: rolling again before choosing a piece is rejected without a crash")
    void rollWhileWaitingForMoveIsRejected() {
        GameEngine engine = TestSupport.newEngine(2, 6);
        engine.rollDice();
        engine.rollDice(); // only one value is scripted: a second real roll would throw

        assertEquals(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase());
        assertEquals(6, engine.state().lastRoll().value());
    }

    @Test
    @DisplayName("NFR-01: choosing a piece before rolling is rejected")
    void moveBeforeRollIsRejected() {
        GameEngine engine = TestSupport.newEngine(2);
        assertFalse(engine.movePiece(GAL_FIRST_PIECE));
        assertTrue(engine.state().piece(GAL_FIRST_PIECE).isInCastle());
    }

    @Test
    @DisplayName("NFR-01: choosing a piece that has no legal move is rejected")
    void pieceWithoutLegalMoveIsRejected() {
        GameEngine engine = TestSupport.newEngine(2, 6);
        engine.rollDice();
        int dorsPiece = 4; // belongs to the other player
        assertFalse(engine.movePiece(dorsPiece));
        assertEquals(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase());
    }

    /** Gal has three pieces home and the fourth {@code stepsBefore} squares before the finish. */
    private static GameEngine galNearFinish(int players, int stepsBefore, Integer... dieValues) {
        GameEngine engine = TestSupport.newEngine(players, dieValues);
        GameState state = engine.state();
        int finish = state.board().finishStep();
        List<Piece> galsPieces = state.piecesOf(state.players().get(0));
        for (int i = 0; i < 3; i++) galsPieces.get(i).moveTo(finish);
        galsPieces.get(3).moveTo(finish - stepsBefore);
        return engine;
    }

    @Test
    @DisplayName("FR-19: a six with no legal move keeps the turn and counts toward the streak")
    void sixWithoutLegalMoveGivesAnotherRollInEngine() {
        GameEngine engine = galNearFinish(2, 2, 6); // a six overshoots the finish
        engine.rollDice();

        GameState state = engine.state();
        assertEquals(0, state.currentPlayerIndex(), "Still Gal's turn");
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
        assertEquals(1, state.consecutiveSixes());
        assertEquals(state.board().finishStep() - 2, state.piece(3).step(), "Nothing moved");
    }

    @Test
    @DisplayName("FR-20: three sixes with no legal move at all still cancel the third and pass the turn")
    void threeSixesWithoutAnyMovePassTurn() {
        GameEngine engine = galNearFinish(2, 2, 6, 6, 6);
        engine.rollDice();
        engine.rollDice();
        engine.rollDice();

        GameState state = engine.state();
        assertEquals(1, state.currentPlayerIndex());
        assertEquals(0, state.consecutiveSixes());
    }

    @Test
    @DisplayName("FR-18: a piece on the board with no legal move does not move and the turn passes")
    void pieceOnBoardWithoutLegalMovePassesTurn() {
        GameEngine engine = galNearFinish(2, 2, 5); // 5 overshoots, the finish needs an exact roll
        engine.rollDice();

        GameState state = engine.state();
        assertEquals(1, state.currentPlayerIndex());
        assertEquals(state.board().finishStep() - 2, state.piece(3).step());
    }

    @Test
    @DisplayName("FR-20: after a cancelled third six, the next player's streak starts from zero")
    void sixStreakDoesNotCarryOverToNextPlayer() {
        GameEngine engine = TestSupport.newEngine(2, 6, 6, 6, 6);
        engine.rollDice(); engine.movePiece(GAL_FIRST_PIECE); // 6: release
        engine.rollDice(); engine.movePiece(GAL_FIRST_PIECE); // 6: move
        engine.rollDice();                                    // 6: third six, cancelled, turn passes
        assertEquals(1, engine.state().currentPlayerIndex());

        engine.rollDice(); // Dor's first six
        assertEquals(1, engine.state().consecutiveSixes(), "Dor's streak starts from zero");
        assertEquals(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase(), "Dor's six is not cancelled");
    }
}
