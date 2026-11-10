package Model;

import java.time.Clock;
import java.util.List;
import java.util.Random;

/** Test that turn-specific die state is separate from the persistent visible result. */
public final class DiceTurnSeparationTest {
    private static int passed;

    /** Fail when a turn-scoped die state differs from the expected state. */
    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Supply predictable dice values while exercising the real game engine. */
    private static GameEngine game(int... rolls) {
        Dice scripted = new Dice(new Random(0)) {
            private int index;
            /** Return the next predetermined die face for the scenario. */
            @Override public DiceRoll roll() {
                if (index >= rolls.length) throw new AssertionError("Unexpected additional roll");
                return new DiceRoll(rolls[index++]);
            }
        };
        return new GameEngine(Difficulty.EASY, List.of("Red", "Blue"),
                List.of(PlayerColor.RED, PlayerColor.BLUE),
                new Random(11), scripted, Clock.systemUTC());
    }

    /** Keep a six visible but clear its active move state after a valid castle release. */
    private static void sixExtraRollDoesNotKeepAnActiveMove() {
        GameEngine engine = game(6);
        engine.rollDice();
        expect(engine.state().lastRoll().value() == 6, "Six must be active before selecting a piece");
        int id = engine.state().legalMoves().get(0).pieceId();
        expect(engine.movePiece(id), "Castle release should succeed");
        expect(engine.state().phase() == GameState.Phase.WAITING_FOR_ROLL,
                "Six must grant an extra roll after a valid move");
        expect(engine.state().lastRoll() == null, "Resolved move must clear the active roll");
        expect(engine.state().latestRoll().value() == 6, "The last physical die face must remain visible");
        expect(engine.state().consecutiveSixes() == 1, "Keeping the die face must not reset the six streak");
        passed++;
    }

    /** Keep the last six visible after an impossible castle release still earns another roll. */
    private static void blockedCastleAndSixShowsPreviousFace() {
        GameEngine engine = game(6);
        GameState state = engine.state();
        Position entry = state.board().positionOf(PlayerColor.RED, 0);
        int blueStep = state.board().route(PlayerColor.BLUE).indexOf(entry);
        expect(blueStep >= 0, "Opponent must traverse the protected entry square");
        List<Piece> enemies = state.piecesOf(state.players().get(1));
        enemies.get(0).moveTo(blueStep);
        enemies.get(1).moveTo(blueStep);
        engine.rollDice();
        expect(state.currentPlayerIndex() == 0, "Six without a legal move earns another roll");
        expect(state.phase() == GameState.Phase.WAITING_FOR_ROLL, "Player should be waiting for another roll");
        expect(state.lastRoll() == null, "No active move means no active roll");
        expect(state.latestRoll().value() == 6, "Cancelled movement must not erase the previous die face");
        passed++;
    }

    /** Preserve the final roll visually while clearing movement permissions when the game ends. */
    private static void gameOverDoesNotKeepActiveRoll() {
        GameEngine engine = game(1);
        GameState state = engine.state();
        List<Piece> red = state.piecesOf(state.players().get(0));
        for (int i = 0; i < 3; i++) red.get(i).moveTo(state.board().finishStep());
        red.get(3).moveTo(state.board().finishStep() - 1);
        engine.rollDice();
        expect(engine.movePiece(red.get(3).id()), "The fourth piece should reach exact finish");
        expect(state.phase() == GameState.Phase.GAME_OVER, "Two-player game should finish");
        expect(state.lastRoll() == null, "Game over must clear actionable roll state");
        expect(state.latestRoll().value() == 1, "Final die face stays on the visual display");
        passed++;
    }

    /** Verify that starting the following player's turn does not erase the previous die value. */
    private static void followingPlayerStartsWithNoActiveRoll() {
        GameEngine engine = game(2, 4);
        engine.rollDice();
        GameState state = engine.state();
        expect(state.currentPlayerIndex() == 1, "First player should pass without legal moves");
        expect(state.lastRoll() == null, "The next player has not rolled yet");
        expect(state.latestRoll().value() == 2, "First result stays visible between turns");
        engine.rollDice();
        expect(state.currentPlayerIndex() == 0, "Second player should also pass without a legal move");
        expect(state.latestRoll().value() == 4, "The next real roll must replace the previous face");
        expect(state.lastRoll() == null, "No active piece movement should remain");
        passed++;
    }

    /** Execute all persistent-visual and active-turn regression scenarios. */
    public static void main(String[] args) {
        sixExtraRollDoesNotKeepAnActiveMove();
        blockedCastleAndSixShowsPreviousFace();
        gameOverDoesNotKeepActiveRoll();
        followingPlayerStartsWithNoActiveRoll();
        System.out.println("PASS: " + passed + " active-roll vs persistent-display checks");
    }
}
