package Model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Shared helpers for the Model unit tests.
 * Tests live in package Model so they can call the package-private rule classes directly.
 */
final class TestSupport {
    private TestSupport() { }

    /** Fixed seed so the random board layout is the same in every run. */
    static final long BOARD_SEED = 42L;

    /**
     * A Random that returns pre-set die values, so a test can build any roll sequence
     * (for example three sixes in a row). Dice calls nextInt(6) and adds 1.
     */
    static final class ScriptedRandom extends Random {
        private final Deque<Integer> values = new ArrayDeque<>();

        ScriptedRandom(Integer... dieValues) {
            values.addAll(Arrays.asList(dieValues));
        }

        @Override
        public int nextInt(int bound) {
            if (values.isEmpty()) throw new IllegalStateException("No scripted die value left");
            return values.removeFirst() - 1;
        }
    }

    /** A clock the test moves forward by hand. */
    static final class ManualClock extends Clock {
        private Instant now = Instant.parse("2026-11-15T10:00:00Z");

        void advance(Duration duration) { now = now.plus(duration); }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneId.of("Asia/Jerusalem"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    /** A state with the given number of players (2-4) on the Easy board. */
    static GameState newState(int playerCount) {
        Board board = BoardFactory.create(Difficulty.EASY, new Random(BOARD_SEED));
        List<PlayerColor> colors = List.of(PlayerColor.values()).subList(0, playerCount);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player(i, "P" + (i + 1), colors.get(i)));
        }
        return new GameState(board, players);
    }

    /** An engine whose die returns exactly the given values, in order. */
    static GameEngine newEngine(int playerCount, Clock clock, Integer... dieValues) {
        List<String> names = List.of("Gal", "Dor", "Omer", "Diana").subList(0, playerCount);
        List<PlayerColor> colors = List.of(PlayerColor.values()).subList(0, playerCount);
        return new GameEngine(Difficulty.EASY, names, colors,
                new Random(BOARD_SEED), new Dice(new ScriptedRandom(dieValues)), clock);
    }

    static GameEngine newEngine(int playerCount, Integer... dieValues) {
        return newEngine(playerCount, new ManualClock(), dieValues);
    }

    /** Puts every piece of the player in the finish area. */
    static void bringAllPiecesHome(GameState state, Player player) {
        for (Piece piece : state.piecesOf(player)) piece.moveTo(state.board().finishStep());
    }
}
