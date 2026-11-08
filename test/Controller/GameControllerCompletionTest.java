package Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Model.ControllerTestFixtures;
import Model.Difficulty;
import Model.GameEngine;
import Model.GameListener;
import Model.GameState;
import Model.PlayerColor;
import View.GameBoardView;
import View.ResultsView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

class GameControllerCompletionTest {

    private GameController controller;
    private JFrame frame;
    private GameEngine engine;

    private Timer dialogResponder;
    private CountDownLatch dialogAnswered;
    private String dialogTitle;
    private String dialogText;
    private String dialogProblem;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Set<Window> existingWindows = new HashSet<>(
                    Arrays.asList(Window.getWindows())
            );

            controller = new GameController();
            controller.start();

            List<JFrame> newFrames = new ArrayList<>();

            for (Window window : Window.getWindows()) {
                if (!existingWindows.contains(window)
                        && window instanceof JFrame candidate) {
                    newFrames.add(candidate);
                }
            }

            assertEquals(1, newFrames.size());
            frame = newFrames.get(0);

            startTwoPlayerGame();
            engine = readControllerEngine();
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (dialogResponder != null) {
                dialogResponder.stop();
            }

            if (frame != null) {
                if (frame.getContentPane()
                        instanceof GameBoardView boardView) {
                    boardView.stopClock();
                }

                for (Window owned : frame.getOwnedWindows()) {
                    owned.dispose();
                }

                frame.dispose();
            }
        });
    }

    @Test
    void winningMoveShowsResultsAfterAnimation()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            GameBoardView boardView = assertInstanceOf(
                    GameBoardView.class,
                    frame.getContentPane()
            );

            int pieceId =
                    ControllerTestFixtures.prepareWinningMove(engine);

            startDialogResponder();

            assertTrue(engine.movePiece(pieceId));

            assertEquals(GameState.Phase.GAME_OVER,
                    engine.state().phase());

            assertSame(boardView, frame.getContentPane(),
                    "יש להמתין לסיום האנימציה לפני החלפת המסך");
        });

        awaitDialog();

        SwingUtilities.invokeAndWait(() -> {
            assertEquals("סיום המשחק", dialogTitle);
            assertTrue(dialogText.contains("שחקן 1"));

            ResultsView results = assertInstanceOf(
                    ResultsView.class,
                    frame.getContentPane()
            );

            List<String> labels =
                    findComponents(results, JLabel.class)
                            .stream()
                            .map(JLabel::getText)
                            .toList();

            assertTrue(labels.contains("מקום 1:  שחקן 1"));

            assertTrue(labels.contains(
                    "מקום 2:  שחקן 2  (לא סיים)"));

            clickButton("חזרה למסך הפתיחה");

            assertInstanceOf(
                    View.MainMenuView.class,
                    frame.getContentPane()
            );
        });
    }

    @Test
    void unexpectedErrorShowsMessageAndKeepsGameWindowOpen()
            throws Exception {

        PrintStream originalErrorStream = System.err;
        ByteArrayOutputStream capturedErrors =
                new ByteArrayOutputStream();

        try (PrintStream testErrorStream = new PrintStream(
                capturedErrors, true, StandardCharsets.UTF_8)) {

            try {
                System.setErr(testErrorStream);

                SwingUtilities.invokeAndWait(() -> {
                    GameBoardView boardView = assertInstanceOf(
                            GameBoardView.class,
                            frame.getContentPane()
                    );

                    GameListener failingListener = new GameListener() {
                        @Override
                        public void onStateChanged(GameState state) {
                            throw new IllegalStateException(
                                    "Controller test failure");
                        }
                    };

                    engine.addListener(failingListener);

                    try {
                        startDialogResponder();
                        clickButton("הטלת קובייה");
                    } finally {
                        engine.removeListener(failingListener);
                    }

                    assertSame(boardView, frame.getContentPane());
                    assertTrue(frame.isShowing());
                });

                awaitDialog();

                SwingUtilities.invokeAndWait(() -> {
                    assertEquals("שגיאה", dialogTitle);

                    assertTrue(dialogText.contains(
                            "אירעה שגיאה לא צפויה"));

                    assertTrue(dialogText.contains(
                            "Controller test failure"));

                    assertTrue(frame.isShowing());

                    assertInstanceOf(
                            GameBoardView.class,
                            frame.getContentPane()
                    );
                });

                testErrorStream.flush();

                String errorOutput =
                        capturedErrors.toString(StandardCharsets.UTF_8);

                assertTrue(errorOutput.contains(
                        "java.lang.IllegalStateException: "
                        + "Controller test failure"),
                        "הבקר צריך לתעד את התקלה הצפויה");
            } finally {
                System.setErr(originalErrorStream);
            }
        }
    }
    private void startTwoPlayerGame() {
        clickButton("משחק חדש");

        Container setup = frame.getContentPane();

        List<JTextField> names =
                findComponents(setup, JTextField.class);

        assertTrue(names.size() >= 2);

        names.get(0).setText("שחקן 1");
        names.get(1).setText("שחקן 2");

        List<JComboBox<?>> colors = new ArrayList<>();

        for (Component component :
                findComponents(setup, Component.class)) {

            if (component instanceof JComboBox<?> box
                    && box.getItemCount() > 0) {

                Object firstItem = box.getItemAt(0);

                if (firstItem instanceof Integer) {
                    box.setSelectedItem(2);
                } else if (firstItem instanceof Difficulty) {
                    box.setSelectedItem(Difficulty.EASY);
                } else if (box.getItemCount() > 1
                        && box.getItemAt(1) instanceof PlayerColor) {
                    colors.add(box);
                }
            }
        }

        assertTrue(colors.size() >= 2);

        PlayerColor[] availableColors = PlayerColor.values();

        colors.get(0).setSelectedItem(availableColors[0]);
        colors.get(1).setSelectedItem(availableColors[1]);

        clickButton("התחלת משחק");

        assertInstanceOf(
                GameBoardView.class,
                frame.getContentPane()
        );
    }

    private GameEngine readControllerEngine() {
        try {
            Field field =
                    GameController.class.getDeclaredField("engine");

            field.setAccessible(true);

            GameEngine result =
                    (GameEngine) field.get(controller);

            assertNotNull(result);

            return result;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Could not access the active game for the test",
                    exception
            );
        }
    }

    private void startDialogResponder() {
        dialogAnswered = new CountDownLatch(1);

        dialogResponder = new Timer(50, event -> {
            for (Window window : frame.getOwnedWindows()) {
                if (window instanceof JDialog dialog
                        && dialog.isShowing()) {

                    ((Timer) event.getSource()).stop();

                    dialogTitle = dialog.getTitle();

                    dialogText =
                            findComponents(dialog, JLabel.class)
                                    .stream()
                                    .map(JLabel::getText)
                                    .filter(Objects::nonNull)
                                    .reduce(
                                            "",
                                            (left, right) ->
                                                    left + "\n" + right
                                    );

                    List<JOptionPane> panes =
                            findComponents(
                                    dialog, JOptionPane.class);

                    if (panes.size() == 1) {
                        panes.get(0).setValue(
                                JOptionPane.OK_OPTION);
                    } else {
                        dialogProblem =
                                "לא נמצא רכיב הודעה יחיד";
                    }

                    dialog.dispose();
                    dialogAnswered.countDown();
                    return;
                }
            }
        });

        dialogResponder.start();
    }

    private void awaitDialog() throws InterruptedException {
        assertTrue(
                dialogAnswered.await(10, TimeUnit.SECONDS),
                "חלון ההודעה צריך להופיע בתוך עשר שניות"
        );

        assertNull(dialogProblem, dialogProblem);
    }

    private void clickButton(String text) {
        List<JButton> buttons =
                findComponents(
                        frame.getContentPane(), JButton.class)
                        .stream()
                        .filter(button ->
                                text.equals(button.getText()))
                        .toList();

        assertEquals(1, buttons.size(),
                "צריך להימצא כפתור אחד עם הטקסט " + text);

        assertTrue(buttons.get(0).isEnabled());
        buttons.get(0).doClick(0);
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