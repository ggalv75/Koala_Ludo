package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import View.DieFace;
import View.GameBoardView;

/**
 * FR-15, FR-16, FR-21, FR-22 on the game board: the multiplier die, the steps on the track,
 * the special-throw button and the lost-turn marker. In the Model package (like
 * PersistentDiceResultTest) because Medium is not selectable yet, so a Boost roll and the
 * surprises are set up through package-private Model calls. Owner: Gal (iteration 2).
 */
class MultiplierDiceViewTest {

    private GameEngine engine;
    private GameBoardView view;

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (view != null) view.stopClock();
        });
    }

    @Test
    @DisplayName("Regression, Easy: no multiplier die, no extra lines, regular roll button")
    void easyBoardHasNoMultiplierDie() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2, 4));
            assertFalse(multiplierDie().isVisible());
            assertFalse(label("multiplier-info").isVisible());
            assertFalse(label("track-steps-info").isVisible());
            assertEquals("הטלת קובייה", rollButton().getText());
        });
    }

    @Test
    @DisplayName("FR-21: a pending surprise throw shows a blank UltraBoost die and names the throw on the button")
    void pendingSpecialRollIsAnnounced() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2, 6, 2));
            TurnManager.grantSpecialRoll(engine.state());
            view.onStateChanged(engine.state());

            DieFace die = multiplierDie();
            assertTrue(die.isVisible());
            assertEquals(6, die.faces());
            assertEquals(0, die.value(), "Nothing rolled yet");
            assertEquals("קוביית UltraBoost", die.getToolTipText());
            assertEquals("הטלה מיוחדת - UltraBoost", rollButton().getText());
        });
    }

    @Test
    @DisplayName("FR-16, FR-21: after the surprise throw both dice and the steps on the track are shown")
    void specialRollShowsBothDice() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2, 6, 2));
            TurnManager.grantSpecialRoll(engine.state());
            engine.rollDice();                      // step die 6, UltraBoost 2 -> coefficient 1

            assertEquals(2, multiplierDie().value());
            assertTrue(labelTexts().contains("תוצאת ההטלה: 6"));
            assertEquals("קוביית UltraBoost: 2", label("multiplier-info").getText());
            assertEquals("צעדים במסלול: 6", label("track-steps-info").getText());
            assertEquals("הטלת קובייה", rollButton().getText(), "The special throw is used");
        });
    }

    @Test
    @DisplayName("FR-15, FR-16: a Boost roll shows a 3-face Boost die and die x (N - 1) steps")
    void boostRollShowsBoostDie() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2));
            showActiveRoll(new DiceRoll(4, MultiplierDie.BOOST, 3));

            DieFace die = multiplierDie();
            assertTrue(die.isVisible());
            assertEquals(3, die.faces());
            assertEquals(3, die.value());
            assertEquals("קוביית Boost", die.getToolTipText());
            assertEquals("קוביית Boost: 3", label("multiplier-info").getText());
            assertEquals("צעדים במסלול: 8", label("track-steps-info").getText());
        });
    }

    @Test
    @DisplayName("FR-16 boundary: multiplier 1 shows 0 steps on the track")
    void coefficientZeroShowsZeroSteps() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2));
            showActiveRoll(new DiceRoll(5, MultiplierDie.BOOST, 1));
            assertEquals("צעדים במסלול: 0", label("track-steps-info").getText());
        });
    }

    @Test
    @DisplayName("At Easy the UltraBoost result stays until the next regular roll, which hides it")
    void regularRollHidesTheMultiplierDieAgain() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2, 3, 5, 2));
            TurnManager.grantSpecialRoll(engine.state());
            engine.rollDice();                      // special 3 + UltraBoost 5, no move: turn passes
            assertEquals(1, engine.state().currentPlayerIndex());
            assertTrue(multiplierDie().isVisible(), "Last result still shown");
            assertEquals(5, multiplierDie().value());

            engine.rollDice();                      // Dor: regular 2, no move
            assertFalse(multiplierDie().isVisible());
            assertFalse(label("multiplier-info").isVisible());
            assertFalse(label("track-steps-info").isVisible());
        });
    }

    @Test
    @DisplayName("FR-22: the player list marks a lost turn until it has been skipped")
    void lostTurnIsMarkedInThePlayerList() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2));
            GameState state = engine.state();
            TurnManager.loseNextTurn(state, state.players().get(1));
            view.onStateChanged(state);
            assertTrue(labelTexts().stream().anyMatch(text -> text.equals("Dor  |  מפסיד/ה תור")));

            TurnManager.passTurn(state);            // Dor is skipped, Gal plays again
            view.onStateChanged(state);
            assertTrue(labelTexts().stream().noneMatch(text -> text.contains("מפסיד/ה תור")));
        });
    }

    @Test
    @DisplayName("FR-21: on the special throw both dice tumble")
    void specialRollAnimatesBothDice() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2, 6, 2));
            TurnManager.grantSpecialRoll(engine.state());
            view.onStateChanged(engine.state());
            rollButton().doClick();
            assertTrue(stepDie().isRolling());
            assertTrue(multiplierDie().isRolling());
        });
    }

    @Test
    @DisplayName("Regression, Easy: a regular roll tumbles only the step die")
    void easyRollAnimatesOnlyTheStepDie() throws Exception {
        onEdt(() -> {
            open(TestSupport.newEngine(2, 4));
            rollButton().doClick();
            assertTrue(stepDie().isRolling());
            assertFalse(multiplierDie().isRolling());
        });
    }

    // ---------- helpers ----------

    private interface EdtAction { void run() throws Exception; }

    /** Swing components are created and checked on the event thread, as in the game. */
    private static void onEdt(EdtAction action) throws Exception {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] instanceof Error error) throw error;
        if (failure[0] instanceof Exception exception) throw exception;
    }

    /** The game screen wired to the engine the same way GameController wires it. */
    private void open(GameEngine gameEngine) {
        engine = gameEngine;
        view = new GameBoardView(engine::elapsedSeconds, engine::rollDice, engine::movePiece, () -> { });
        engine.addListener(view);
        view.onStateChanged(engine.state());
    }

    /** A roll waiting for a move, as the engine leaves it after a throw with a legal move. */
    private void showActiveRoll(DiceRoll roll) {
        GameState state = engine.state();
        state.setLastRoll(roll);
        state.setPhase(GameState.Phase.WAITING_FOR_MOVE);
        view.onStateChanged(state);
    }

    private DieFace multiplierDie() { return (DieFace) named(view, "multiplier-die"); }

    private JLabel label(String name) { return (JLabel) named(view, name); }

    private JButton rollButton() { return (JButton) named(view, "roll-button"); }

    /** The step die is the only unnamed die face on the board. */
    private DieFace stepDie() {
        return allOf(view, DieFace.class).stream()
                .filter(die -> die.getName() == null)
                .findFirst().orElseThrow();
    }

    private List<String> labelTexts() {
        return allOf(view, JLabel.class).stream().map(JLabel::getText).toList();
    }

    private static Component named(Container parent, String name) {
        for (Component component : parent.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container nested) {
                Component found = named(nested, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> List<T> allOf(Container parent, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) found.add(type.cast(component));
            if (component instanceof Container nested) found.addAll(allOf(nested, type));
        }
        return found;
    }
}
