package Model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

/** Helpers shared by the unit tests. */
final class TestSupport {
    static final List<PlayerColor> COLORS =
            List.of(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE);

    private TestSupport() { }

    static List<String> names(int players) {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= players; i++) names.add("P" + i);
        return names;
    }

    /** Easy-level state on a seeded board; every piece starts in its castle. */
    static GameState state(int players, long seed) {
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < players; i++) list.add(new Player(i, "P" + (i + 1), COLORS.get(i)));
        return new GameState(BoardFactory.create(Difficulty.EASY, new Random(seed)), list);
    }

    /** Engine with scripted dice; the board is seeded so it is the same every run. */
    static GameEngine engine(int players, Dice dice) {
        return engine(players, dice, Clock.systemUTC());
    }

    static GameEngine engine(int players, Dice dice, Clock clock) {
        return new GameEngine(Difficulty.EASY, names(players), COLORS.subList(0, players),
                new Random(7), dice, clock);
    }

    /** Dice that return the given values in order and fail the test if rolled more often. */
    static Dice scriptedDice(int... values) {
        Deque<Integer> queue = new ArrayDeque<>();
        for (int v : values) queue.add(v);
        return new Dice(new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) {
                if (queue.isEmpty()) throw new AssertionError("The die was rolled more times than scripted");
                return queue.removeFirst() - 1;
            }
        });
    }

    static Piece piece(GameState state, int player, int number) {
        return state.piecesOf(state.players().get(player)).get(number - 1);
    }

    /** Step on {@code color}'s route of the square that is {@code step} on {@code reference}'s route. */
    static int sameSquare(Board board, PlayerColor color, PlayerColor reference, int step) {
        int index = board.route(color).indexOf(board.positionOf(reference, step));
        if (index < 0) throw new IllegalArgumentException("Square is not on the route of " + color);
        return index;
    }

    /** First REGULAR square on the colour's route after {@code from}. */
    static int regularStepAfter(Board board, PlayerColor color, int from) {
        for (int step = from + 1; step < board.homePathStart(); step++) {
            if (board.cellAt(color, step).type() == CellType.REGULAR) return step;
        }
        throw new IllegalStateException("No regular square found");
    }

    /** A clock the test can move forward. */
    static final class ManualClock extends Clock {
        private Instant now = Instant.parse("2026-11-15T10:00:00Z");

        void advance(Duration duration) { now = now.plus(duration); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
