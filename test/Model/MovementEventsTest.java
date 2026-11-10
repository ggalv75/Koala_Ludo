package Model;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Model regression: presentation notifications must not change Piece Movement rules. */
public final class MovementEventsTest {
    private static int passed;

    /** Create a deterministic game engine for observer checks. */
    private static GameEngine newEngine(int... rolls) {
        Dice dice = new Dice(new Random()) {
            private int index;
            /** Produce the next scripted die result. */
            @Override public DiceRoll roll() { return new DiceRoll(rolls[index++]); }
        };
        return new GameEngine(Difficulty.EASY, List.of("Adi", "Dana"),
                List.of(PlayerColor.RED, PlayerColor.GREEN), new Random(8), dice, Clock.systemUTC());
    }

    /** Assert a test expectation and provide a failure message. */
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** Run and report one isolated test scenario. */
    private static void test(String name, Runnable action) {
        action.run();
        passed++;
        System.out.println("PASS " + name);
    }

    /** Run the test cases and report successful assertions. */
    public static void main(String[] args) {
        test("release emits Move before state notification", () -> {
            GameEngine engine = newEngine(6);
            List<String> order = new ArrayList<>();
            List<Move> moves = new ArrayList<>();
            engine.addListener(new GameListener() {
                /** Record the committed move for observer-order assertions. */
                @Override public void onPieceMoved(Move move) { moves.add(move); order.add("MOVE"); }
                /** Record the notified state for observer-order assertions. */
                @Override public void onStateChanged(GameState state) { order.add("STATE"); }
            });
            engine.rollDice();
            order.clear();
            check(engine.movePiece(0), "legal castle exit");
            check(order.equals(List.of("MOVE", "STATE")), "move event must precede state change: " + order);
            check(moves.size() == 1 && moves.get(0).release(), "castle release reported");
            check(engine.state().piece(0).step() == 0, "source of truth remains in Model");
        });
        test("invalid selection never emits a move", () -> {
            GameEngine engine = newEngine(6);
            List<Move> moves = new ArrayList<>();
            engine.addListener(new GameListener() {
                /** Record the committed move for observer-order assertions. */
                @Override public void onPieceMoved(Move move) { moves.add(move); }
            });
            engine.rollDice();
            check(!engine.movePiece(600), "invalid piece selection rejected");
            check(moves.isEmpty(), "no movement event for illegal action");
        });
        test("normal move event preserves route steps", () -> {
            GameEngine engine = newEngine(6, 3);
            List<Move> moves = new ArrayList<>();
            engine.addListener(new GameListener() {
                /** Record the committed move for observer-order assertions. */
                @Override public void onPieceMoved(Move move) { moves.add(move); }
            });
            engine.rollDice(); engine.movePiece(0);
            engine.rollDice(); engine.movePiece(0);
            check(moves.size() == 2, "two committed moves");
            check(moves.get(1).fromStep() == 0 && moves.get(1).toStep() == 3, "exact route positions");
            check(engine.state().piece(0).step() == 3, "actual piece advanced by 3");
        });
        test("last move event delivered before GAME_OVER event", () -> {
            GameEngine engine = newEngine(1);
            GameState state = engine.state();
            for (int id = 0; id < 3; id++) state.piece(id).moveTo(state.board().finishStep());
            state.piece(3).moveTo(state.board().finishStep() - 1);
            List<String> order = new ArrayList<>();
            engine.addListener(new GameListener() {
                /** Record the committed move for observer-order assertions. */
                @Override public void onPieceMoved(Move move) { order.add("MOVE"); }
                /** Record the notified state for observer-order assertions. */
                @Override public void onStateChanged(GameState s) { order.add("STATE"); }
                /** Record the game-over callback in the scripted scenario. */
                @Override public void onGameOver(GameResult result) { order.add("OVER"); }
            });
            engine.rollDice(); order.clear();
            check(engine.movePiece(3), "last piece reaches finish");
            check(order.equals(List.of("MOVE", "STATE", "OVER")), "final animation event precedes results: " + order);
            check(state.phase() == GameState.Phase.GAME_OVER, "game over did not change");
        });
        System.out.printf("RESULT MovementEventsTest: %d/%d passed%n", passed, 4);
    }
}
