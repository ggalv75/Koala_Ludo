package Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
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

import Model.PlayerColor;
import View.GameBoardView;
import View.GameSetupView;
import View.MainMenuView;

class GameControllerIntegrationTest {

    private JFrame frame;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Set<Window> existingWindows = new HashSet<>(
                    Arrays.asList(Window.getWindows())
            );

            GameController controller = new GameController();
            controller.start();

            List<JFrame> newFrames = new ArrayList<>();

            for (Window window : Window.getWindows()) {
                if (!existingWindows.contains(window)
                        && window instanceof JFrame candidate) {
                    newFrames.add(candidate);
                }
            }

            assertEquals(1, newFrames.size(),
                    "הבקר צריך ליצור חלון משחק אחד");

            frame = newFrames.get(0);

            assertTrue(frame.isShowing());
            assertInstanceOf(MainMenuView.class,
                    frame.getContentPane());
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
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
    void opensSetupAndReturnsToMainMenu() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickButton("menu-new-game");

            assertInstanceOf(GameSetupView.class,
                    frame.getContentPane());

            clickButton("setup-back");

            assertInstanceOf(MainMenuView.class,
                    frame.getContentPane());
        });
    }

    /** FR-05: a game starts for every supported player count (2-4). */
    @Test
    void startsGameForSupportedPlayerCounts() throws Exception {
        for (int playerCount = 2; playerCount <= 4; playerCount++) {
            final int count = playerCount;

            SwingUtilities.invokeAndWait(() -> {
                openAndFillSetup(count);

                clickButton("setup-start");

                assertInstanceOf(
                        GameBoardView.class,
                        frame.getContentPane(),
                        "המשחק צריך להתחיל עבור " + count + " שחקנים"
                );

                assertTrue(findNamed(AbstractButton.class,
                                "roll-button").isEnabled(),
                        "משחק חדש צריך לאפשר הטלה ראשונה");

                clickAndAnswerDialog(
                        "board-exit",
                        "יציאה מהמשחק",
                        JOptionPane.YES_OPTION
                );

                assertInstanceOf(MainMenuView.class,
                        frame.getContentPane());
            });
        }
    }

    @Test
    void missingNameShowsInlineErrorAndKeepsSetupScreen()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            openAndFillSetup(2);

            GameSetupView setup = assertInstanceOf(
                    GameSetupView.class,
                    frame.getContentPane()
            );

            findNamed(JTextField.class, "player-name-0").setText("");

            clickButton("setup-start");

            assertSame(setup, frame.getContentPane(),
                    "קלט שגוי צריך להשאיר את המשתמש בהגדרות");

            assertSetupErrorShown();
        });
    }

    @Test
    void takenColorCannotBeSelectedByAnotherPlayer()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            openAndFillSetup(2);

            GameSetupView setup = assertInstanceOf(
                    GameSetupView.class,
                    frame.getContentPane()
            );

            PlayerColor[] colors = PlayerColor.values();

            AbstractButton takenColor = findNamed(AbstractButton.class,
                    colorButtonName(1, colors[0]));
            AbstractButton ownColor = findNamed(AbstractButton.class,
                    colorButtonName(1, colors[1]));

            assertFalse(takenColor.isEnabled(),
                    "צבע של שחקן אחר צריך להיות חסום");

            takenColor.doClick(0);

            assertFalse(takenColor.isSelected(),
                    "לחיצה על צבע תפוס לא משנה את הבחירה");
            assertTrue(ownColor.isSelected(),
                    "הצבע הקודם של השחקן נשמר");

            // Deselect player 2's color: starting without a color must fail.
            ownColor.doClick(0);
            assertFalse(ownColor.isSelected());

            clickButton("setup-start");

            assertSame(setup, frame.getContentPane(),
                    "שחקן בלי צבע לא יכול להתחיל משחק");

            assertSetupErrorShown();
        });
    }

    @Test
    void cancelKeepsGameAndConfirmReturnsToMainMenu()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            openAndFillSetup(2);
            clickButton("setup-start");

            GameBoardView boardView = assertInstanceOf(
                    GameBoardView.class,
                    frame.getContentPane()
            );

            clickAndAnswerDialog(
                    "board-exit",
                    "יציאה מהמשחק",
                    JOptionPane.NO_OPTION
            );

            assertSame(boardView, frame.getContentPane(),
                    "ביטול היציאה צריך להשאיר את המשחק פתוח");

            clickAndAnswerDialog(
                    "board-exit",
                    "יציאה מהמשחק",
                    JOptionPane.YES_OPTION
            );

            assertInstanceOf(MainMenuView.class,
                    frame.getContentPane());
        });
    }

    @Test
    void rulesDialogKeepsMainMenuOpen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainMenuView menu = assertInstanceOf(
                    MainMenuView.class,
                    frame.getContentPane()
            );

            clickAndAnswerDialog(
                    "menu-rules",
                    "חוקי המשחק - רמה קלה",
                    JOptionPane.OK_OPTION
            );

            assertSame(menu, frame.getContentPane());
        });
    }

    /** Opens the setup screen and fills valid names and unique colors. */
    private void openAndFillSetup(int playerCount) {
        clickButton("menu-new-game");

        assertInstanceOf(GameSetupView.class, frame.getContentPane());

        clickButton("player-count-" + playerCount);

        PlayerColor[] availableColors = PlayerColor.values();

        for (int i = 0; i < playerCount; i++) {
            findNamed(JTextField.class, "player-name-" + i)
                    .setText("שחקן " + (i + 1));

            clickButton(colorButtonName(i, availableColors[i]));
        }
    }

    private static String colorButtonName(int player, PlayerColor color) {
        return "player-color-" + player + "-" + color.name();
    }

    private void assertSetupErrorShown() {
        JLabel error = findNamed(JLabel.class, "setup-error");

        assertTrue(error.isVisible(),
                "הודעת השגיאה צריכה להופיע במסך ההגדרות");
        assertFalse(error.getText().isBlank(),
                "הודעת השגיאה צריכה להסביר מה חסר");
    }

    private void clickButton(String name) {
        AbstractButton button = findNamed(AbstractButton.class, name);

        assertTrue(button.isEnabled(),
                "הכפתור צריך להיות פעיל " + name);

        button.doClick(0);
    }

    /** Finds exactly one component by its stable name, not its visible text. */
    private <T extends Component> T findNamed(Class<T> type, String name) {
        List<T> matches =
                findComponents(frame.getContentPane(), type)
                        .stream()
                        .filter(component ->
                                name.equals(component.getName()))
                        .toList();

        assertEquals(1, matches.size(),
                "צריך להימצא רכיב אחד עם המזהה " + name);

        return matches.get(0);
    }

    private void clickAndAnswerDialog(
            String buttonName,
            String expectedTitle,
            int answer) {

        AtomicReference<String> actualTitle =
                new AtomicReference<>();

        AtomicReference<String> dialogProblem =
                new AtomicReference<>();

        Timer responder = new Timer(50, event -> {
            for (Window window : frame.getOwnedWindows()) {
                if (window instanceof JDialog dialog
                        && dialog.isShowing()) {

                    ((Timer) event.getSource()).stop();
                    actualTitle.set(dialog.getTitle());

                    List<JOptionPane> panes =
                            findComponents(
                                    dialog, JOptionPane.class);

                    if (panes.size() == 1) {
                        panes.get(0).setValue(answer);
                    } else {
                        dialogProblem.set(
                                "לא נמצא רכיב הודעה יחיד בחלון");
                    }

                    dialog.dispose();
                    return;
                }
            }
        });

        responder.start();

        try {
            clickButton(buttonName);
        } finally {
            responder.stop();
        }

        assertNull(dialogProblem.get(), dialogProblem.get());

        assertEquals(expectedTitle, actualTitle.get(),
                "צריך להיפתח חלון ההודעה או האישור המתאים");
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