package View;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.GameEngine;
import Model.PlayerColor;

class GameBoardViewTest {

    private GameBoardView view;
    private GameEngine engine;
    private AtomicLong elapsedSeconds;

    private int rollCalls;
    private int exitCalls;
    private int pieceClickCalls;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PlayerColor[] colors = PlayerColor.values();

            engine = new GameEngine(
                    Difficulty.EASY,
                    List.of("דור", "גל"),
                    List.of(colors[0], colors[1])
            );

            elapsedSeconds = new AtomicLong(65);

            view = new GameBoardView(
                    elapsedSeconds::get,
                    () -> rollCalls++,
                    pieceId -> pieceClickCalls++,
                    () -> exitCalls++
            );

            view.onStateChanged(engine.state());
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
    void displaysInitialGameInformation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<String> texts = labelTexts();

            assertTrue(texts.contains(
                    "רמת קושי: " + Difficulty.EASY.displayName()));

            assertTrue(texts.contains("התור של דור"));
            assertTrue(texts.contains("ממתין להטלה"));
            assertTrue(texts.contains("דור  |  בבית: 0/4"));
            assertTrue(texts.contains("גל  |  בבית: 0/4"));

            assertTrue(findButton("הטלת קובייה").isEnabled());

            assertEquals(0, rollCalls);
            assertEquals(0, exitCalls);
            assertEquals(0, pieceClickCalls);
        });
    }

    @Test
    void refreshesDisplayedTimeFromSuppliedElapsedSeconds()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(labelTexts().contains("00:01:05"));

            elapsedSeconds.set(3661);
            view.onStateChanged(engine.state());

            List<String> texts = labelTexts();

            assertTrue(texts.contains("01:01:01"),
                    "התצוגה צריכה להתעדכן לזמן החדש");

            assertFalse(texts.contains("00:01:05"),
                    "הזמן הקודם צריך להתחלף");
        });
    }

    @Test
    void rollButtonInvokesOnlyRollAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton rollButton = findButton("הטלת קובייה");

            assertTrue(rollButton.isEnabled());

            rollButton.doClick(0);

            assertEquals(1, rollCalls);
            assertEquals(0, exitCalls);
            assertEquals(0, pieceClickCalls);
        });
    }

    @Test
    void exitButtonInvokesOnlyExitAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton exitButton =
                    findButton("יציאה למסך הפתיחה");

            assertTrue(exitButton.isEnabled());

            exitButton.doClick(0);

            assertEquals(1, exitCalls);
            assertEquals(0, rollCalls);
            assertEquals(0, pieceClickCalls);
        });
    }

    @Test
    void displaysGameLogAsReadOnly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<JTextArea> textAreas =
                    findComponents(view, JTextArea.class);

            assertEquals(1, textAreas.size());

            JTextArea log = textAreas.get(0);

            assertFalse(log.isEditable(),
                    "המשתמש לא אמור לערוך את יומן המשחק");

            assertFalse(log.getText().isBlank(),
                    "היומן צריך להציג את הודעת תחילת המשחק");

            assertEquals(
                    String.join("\n", engine.state().recentLog()),
                    log.getText()
            );
        });
    }

    @Test
    void timerAutomaticallyUpdatesDisplayedTime() throws Exception {
        CountDownLatch timerReadTime = new CountDownLatch(1);
        GameBoardView[] timedView = new GameBoardView[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                timedView[0] = new GameBoardView(
                        () -> {
                            timerReadTime.countDown();
                            return 3661L;
                        },
                        () -> { },
                        pieceId -> { },
                        () -> { }
                );
            });

            assertTrue(timerReadTime.await(5, TimeUnit.SECONDS),
                    "הטיימר צריך לבקש את זמן המשחק בתוך חמש שניות");

            SwingUtilities.invokeAndWait(() -> {
                List<String> texts =
                        findComponents(timedView[0], JLabel.class)
                                .stream()
                                .map(JLabel::getText)
                                .toList();

                assertTrue(texts.contains("01:01:01"),
                        "השעון צריך להתעדכן אוטומטית לזמן שסופק");
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (timedView[0] != null) {
                    timedView[0].stopClock();
                }
            });
        }
    }

    private List<String> labelTexts() {
        return findComponents(view, JLabel.class)
                .stream()
                .map(JLabel::getText)
                .toList();
    }

    private JButton findButton(String text) {
        List<JButton> matches =
                findComponents(view, JButton.class)
                        .stream()
                        .filter(button -> text.equals(button.getText()))
                        .toList();

        assertEquals(1, matches.size(),
                "צריך להימצא כפתור אחד עם הטקסט " + text);

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