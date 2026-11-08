package View;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.GameEngine;
import Model.GameState;
import Model.Player;
import Model.PlayerColor;
import Model.PlayerFinishEvent;

class GameBoardAnimationTest {

    private GameBoardView view;
    private GameState state;
    private int rollCalls;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PlayerColor[] colors = PlayerColor.values();

            GameEngine engine = new GameEngine(
                    Difficulty.EASY,
                    List.of("דור", "גל"),
                    List.of(colors[0], colors[1])
            );

            state = engine.state();

            view = new GameBoardView(
                    () -> 65L,
                    () -> rollCalls++,
                    pieceId -> { },
                    () -> { }
            );

            view.onStateChanged(state);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (view != null) {
                view.stopClock();
            }
        });
    }

    @Test
    void rollButtonIsDisabledDuringAnimationAndIgnoresClicks()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            JButton rollButton = findRollButton();

            assertEquals(GameState.Phase.WAITING_FOR_ROLL,
                    state.phase());
            assertTrue(rollButton.isEnabled());

            startFinishAnimation();

            assertTrue(findCanvas().isBusy());
            assertFalse(rollButton.isEnabled(),
                    "ההטלה צריכה להיחסם בזמן אנימציה");

            rollButton.doClick(0);

            assertEquals(0, rollCalls,
                    "כפתור מושבת לא אמור להפעיל הטלה");
        });
    }

    @Test
    void disablingAnimationsReenablesRollButton()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            JButton rollButton = findRollButton();
            JCheckBox animations = findAnimationsCheckbox();

            assertTrue(animations.isSelected());

            startFinishAnimation();

            assertTrue(findCanvas().isBusy());
            assertFalse(rollButton.isEnabled());

            animations.doClick(0);

            assertFalse(animations.isSelected());
            assertFalse(findCanvas().isBusy());

            assertEquals(GameState.Phase.WAITING_FOR_ROLL,
                    state.phase());

            assertTrue(rollButton.isEnabled(),
                    "לאחר כיבוי האנימציה, ניתן להטיל בתור המתאים");

            rollButton.doClick(0);

            assertEquals(1, rollCalls);
        });
    }

    @Test
    void naturalAnimationCompletionReenablesRollButton()
            throws Exception {

        CountDownLatch finished = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            startFinishAnimation();

            assertTrue(findCanvas().isBusy());
            assertFalse(findRollButton().isEnabled());

            view.whenVisualEffectsFinish(finished::countDown);
        });

        assertTrue(finished.await(7, TimeUnit.SECONDS),
                "האנימציה צריכה להסתיים בתוך שבע שניות");

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(findCanvas().isBusy());

            assertEquals(GameState.Phase.WAITING_FOR_ROLL,
                    state.phase());

            JButton rollButton = findRollButton();

            assertTrue(rollButton.isEnabled(),
                    "בסיום האנימציה, כפתור ההטלה צריך להתעדכן");

            rollButton.doClick(0);
        });

        // With animations on, the roll reaches the Controller after the die stops.
        awaitRollCalls(1);
    }

    private void startFinishAnimation() {
        Player player = state.currentPlayer();

        PlayerFinishEvent event = new PlayerFinishEvent(
                player.index(),
                player.name(),
                player.color(),
                1
        );

        view.onPlayerFinished(event);
        view.onStateChanged(state);
    }

    private JButton findRollButton() {
        List<JButton> matches =
                findComponents(view, JButton.class)
                        .stream()
                        .filter(button ->
                                "roll-button".equals(button.getName()))
                        .toList();

        assertEquals(1, matches.size(),
                "צריך להימצא כפתור הטלה אחד");

        return matches.get(0);
    }

    private JCheckBox findAnimationsCheckbox() {
        List<JCheckBox> matches =
                findComponents(view, JCheckBox.class)
                        .stream()
                        .filter(checkbox ->
                                "animations-toggle"
                                        .equals(checkbox.getName()))
                        .toList();

        assertEquals(1, matches.size(),
                "צריכה להימצא תיבת בחירה אחת לאנימציות");

        return matches.get(0);
    }

    /** Polls on the EDT until the animated roll reaches the Controller. */
    private void awaitRollCalls(int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        int[] current = new int[1];

        do {
            SwingUtilities.invokeAndWait(() -> current[0] = rollCalls);

            if (current[0] >= expected) {
                return;
            }

            Thread.sleep(20);
        } while (System.currentTimeMillis() < deadline);

        fail("ההטלה לא נשלחה בתוך חמש שניות");
    }

    private BoardCanvas findCanvas() {
        List<BoardCanvas> matches =
                findComponents(view, BoardCanvas.class);

        assertEquals(1, matches.size(),
                "צריך להימצא לוח אחד");

        return matches.get(0);
    }

    private <T extends Component> List<T> findComponents(
            Container container, Class<T> type) {

        List<T> matches = new ArrayList<>();

        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }

            if (component instanceof Container childContainer) {
                matches.addAll(findComponents(childContainer, type));
            }
        }

        return matches;
    }
}