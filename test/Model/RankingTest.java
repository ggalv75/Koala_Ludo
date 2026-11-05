package Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** End of game, ranking and duration, FR-45 to FR-49 (SDP 5.2.1: Gal). */
class RankingTest {

    /** Puts three of the player's pieces in the finish area and the fourth {@code stepsLeft} before it. */
    private static Piece almostDone(GameState state, int player, int stepsLeft) {
        int finish = state.board().finishStep();
        for (int n = 1; n <= 3; n++) TestSupport.piece(state, player, n).moveTo(finish);
        Piece last = TestSupport.piece(state, player, 4);
        last.moveTo(finish - stepsLeft);
        return last;
    }

    @Test
    @DisplayName("FR-45 / FR-47: places follow the finishing order; the last player is ranked last without finishing")
    void rankingAndGameOver() {
        GameEngine engine = TestSupport.engine(3, TestSupport.scriptedDice(2, 3));
        GameState state = engine.state();
        List<GameResult> results = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onGameOver(GameResult result) { results.add(result); }
        });
        Piece red = almostDone(state, 0, 2);
        Piece green = almostDone(state, 1, 3);

        engine.rollDice();
        engine.movePiece(red.id());
        assertEquals(1, state.players().get(0).rank());
        assertEquals(1, state.currentPlayerIndex(), "Green plays next");
        assertNotEquals(GameState.Phase.GAME_OVER, state.phase());

        engine.rollDice();
        engine.movePiece(green.id());

        assertEquals(GameState.Phase.GAME_OVER, state.phase());
        assertEquals(1, results.size(), "Listeners are told once that the game ended");
        GameResult result = results.get(0);
        assertEquals(List.of("P1", "P2", "P3"),
                result.standings().stream().map(GameResult.Standing::name).toList());
        assertTrue(result.standings().get(0).completedAllPieces());
        assertTrue(result.standings().get(1).completedAllPieces());
        assertFalse(result.standings().get(2).completedAllPieces(), "FR-47: last player did not finish");
        assertEquals(3, result.standings().get(2).rank());
        assertEquals(Difficulty.EASY, result.difficulty());
    }

    @Test
    @DisplayName("FR-46: a player who finishes with a six gets no extra roll")
    void noExtraRollAfterFinishing() {
        GameEngine engine = TestSupport.engine(3, TestSupport.scriptedDice(6));
        GameState state = engine.state();
        Piece red = almostDone(state, 0, 6);
        engine.rollDice();
        engine.movePiece(red.id());
        assertTrue(state.players().get(0).isFinished());
        assertEquals(1, state.currentPlayerIndex());
    }

    @Test
    @DisplayName("FR-49: after the game ends no game action is possible")
    void noActionsAfterGameOver() {
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(1));
        GameState state = engine.state();
        Piece red = almostDone(state, 0, 1);
        engine.rollDice();
        engine.movePiece(red.id());
        assertEquals(GameState.Phase.GAME_OVER, state.phase());

        engine.rollDice(); // the scripted die would fail if it were rolled
        assertFalse(engine.movePiece(TestSupport.piece(state, 1, 1).id()));
        assertEquals(GameState.Phase.GAME_OVER, state.phase());
    }

    @Test
    @DisplayName("FR-48: duration is measured from start to end and stops when the game ends")
    void durationStopsAtEnd() {
        TestSupport.ManualClock clock = new TestSupport.ManualClock();
        GameEngine engine = TestSupport.engine(2, TestSupport.scriptedDice(1), clock);
        Piece red = almostDone(engine.state(), 0, 1);
        clock.advance(Duration.ofSeconds(65));
        assertEquals(65, engine.elapsedSeconds());

        engine.rollDice();
        engine.movePiece(red.id());
        clock.advance(Duration.ofSeconds(100));

        assertEquals(65, engine.elapsedSeconds());
        assertEquals("00:01:05", engine.state().result().formattedDuration());
    }

    @Test
    @DisplayName("FR-48: HH:MM:SS format (boundary values)")
    void durationFormat() {
        assertEquals("00:00:00", GameClock.format(0));
        assertEquals("00:00:59", GameClock.format(59));
        assertEquals("00:01:00", GameClock.format(60));
        assertEquals("01:00:00", GameClock.format(3600));
        assertEquals("01:02:05", GameClock.format(3725));
    }
}
