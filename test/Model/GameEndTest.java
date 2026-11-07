package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FR-45 to FR-48 through the GameEngine: the last piece of a player reaches home
 * and the game ends. Owner: Gal (SDP 5.2.1).
 */
class GameEndTest {

    /** Gal has three pieces home and the fourth one square before the finish; Dor is in the castle. */
    private static GameEngine galOneStepFromWinning(TestSupport.ManualClock time) {
        GameEngine engine = TestSupport.newEngine(2, time, 1);
        GameState state = engine.state();
        int finish = state.board().finishStep();
        List<Piece> galsPieces = state.piecesOf(state.players().get(0));
        for (int i = 0; i < 3; i++) galsPieces.get(i).moveTo(finish);
        galsPieces.get(3).moveTo(finish - 1);
        return engine;
    }

    @Test
    @DisplayName("FR-45, FR-47: the last piece home ends a 2-player game and Gal wins")
    void lastPieceHomeEndsGame() {
        GameEngine engine = galOneStepFromWinning(new TestSupport.ManualClock());
        engine.rollDice();
        engine.movePiece(3);

        GameState state = engine.state();
        assertEquals(GameState.Phase.GAME_OVER, state.phase());
        assertEquals("Gal", state.result().winner().name());
        assertEquals(1, state.players().get(0).rank());
        assertEquals(2, state.players().get(1).rank());
        assertFalse(state.result().standings().get(1).completedAllPieces());
    }

    @Test
    @DisplayName("FR-48: the result keeps the game duration and the clock stops")
    void resultKeepsGameDuration() {
        TestSupport.ManualClock time = new TestSupport.ManualClock();
        GameEngine engine = galOneStepFromWinning(time);
        time.advance(Duration.ofSeconds(3725));
        engine.rollDice();
        engine.movePiece(3);
        time.advance(Duration.ofMinutes(10));

        assertEquals("01:02:05", engine.state().result().formattedDuration());
        assertEquals(3725, engine.elapsedSeconds());
        assertEquals(Difficulty.EASY, engine.state().result().difficulty());
    }

    @Test
    @DisplayName("FR-47: the views are told once that the game is over")
    void listenersAreNotifiedOnce() {
        GameEngine engine = galOneStepFromWinning(new TestSupport.ManualClock());
        List<GameResult> results = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onGameOver(GameResult result) { results.add(result); }
        });

        engine.rollDice();
        engine.movePiece(3);

        assertEquals(1, results.size());
        assertEquals("Gal", results.get(0).winner().name());
    }

    @Test
    @DisplayName("FR-47, NFR-01: after the game ends, rolling and moving are blocked without a crash")
    void finishedGameIsLocked() {
        GameEngine engine = galOneStepFromWinning(new TestSupport.ManualClock());
        engine.rollDice();
        engine.movePiece(3);

        engine.rollDice(); // only one value is scripted: a real roll here would throw
        assertFalse(engine.movePiece(4));
        assertEquals(GameState.Phase.GAME_OVER, engine.state().phase());
    }

    @Test
    @DisplayName("FR-46: with 3 players the game goes on and the finished player is skipped")
    void gameContinuesAfterFirstFinisherWithThreePlayers() {
        GameEngine engine = TestSupport.newEngine(3, 1, 2);
        GameState state = engine.state();
        int finish = state.board().finishStep();
        List<Piece> galsPieces = state.piecesOf(state.players().get(0));
        for (int i = 0; i < 3; i++) galsPieces.get(i).moveTo(finish);
        galsPieces.get(3).moveTo(finish - 1);

        engine.rollDice();
        engine.movePiece(3);
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, state.phase());
        assertEquals(1, state.currentPlayerIndex());

        engine.rollDice(); // Dor rolls 2 with no piece out: the turn passes over Gal to Omer
        assertEquals(2, state.currentPlayerIndex());
    }
}
