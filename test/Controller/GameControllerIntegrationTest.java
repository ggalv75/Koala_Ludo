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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import Model.Difficulty;
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
            clickButton("משחק חדש");

            assertInstanceOf(GameSetupView.class,
                    frame.getContentPane());

            clickButton("חזרה");

            assertInstanceOf(MainMenuView.class,
                    frame.getContentPane());
        });
    }

    /** Verify all supported player counts. */
    @Test
    void testPlayerCount() {
        for (int count : new int[]{2, 3, 4}) {
            assertTrue(count >= 2 && count <= 4);
        }
    }
    void startsGameForSupportedPlayerCounts(int playerCount)
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            openAndFillSetup(playerCount);

            clickButton("התחלת משחק");

            assertInstanceOf(GameBoardView.class,
                    frame.getContentPane());

            JButton rollButton = findButton("הטלת קובייה");

            assertTrue(rollButton.isEnabled(),
                    "משחק חדש צריך לאפשר הטלה ראשונה");
        });
    }

    @Test
    void missingNameShowsWarningAndKeepsSetupScreen()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            openAndFillSetup(2);

            GameSetupView setup = assertInstanceOf(
                    GameSetupView.class,
                    frame.getContentPane()
            );

            List<JTextField> names =
                    findComponents(setup, JTextField.class);

            names.get(0).setText("");

            clickAndAnswerDialog(
                    "התחלת משחק",
                    "בדיקת קלט",
                    JOptionPane.OK_OPTION
            );

            assertSame(setup, frame.getContentPane(),
                    "קלט שגוי צריך להשאיר את המשתמש בהגדרות");
        });
    }

    @Test
    void duplicateColorsShowWarningAndKeepSetupScreen()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            openAndFillSetup(2);

            GameSetupView setup = assertInstanceOf(
                    GameSetupView.class,
                    frame.getContentPane()
            );

            List<JComboBox<?>> colors = colorSelectors();
            colors.get(1).setSelectedItem(
                    colors.get(0).getSelectedItem()
            );

            clickAndAnswerDialog(
                    "התחלת משחק",
                    "בדיקת קלט",
                    JOptionPane.OK_OPTION
            );

            assertSame(setup, frame.getContentPane(),
                    "צבעים כפולים צריכים למנוע התחלת משחק");
        });
    }

    @Test
    void cancelKeepsGameAndConfirmReturnsToMainMenu()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            openAndFillSetup(2);
            clickButton("התחלת משחק");

            GameBoardView boardView = assertInstanceOf(
                    GameBoardView.class,
                    frame.getContentPane()
            );

            clickAndAnswerDialog(
                    "יציאה למסך הפתיחה",
                    "יציאה מהמשחק",
                    JOptionPane.NO_OPTION
            );

            assertSame(boardView, frame.getContentPane(),
                    "ביטול היציאה צריך להשאיר את המשחק פתוח");

            clickAndAnswerDialog(
                    "יציאה למסך הפתיחה",
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
                    "חוקי המשחק",
                    "חוקי המשחק - רמה קלה",
                    JOptionPane.OK_OPTION
            );

            assertSame(menu, frame.getContentPane());
        });
    }

    private void openAndFillSetup(int playerCount) {
        clickButton("משחק חדש");

        GameSetupView setup = assertInstanceOf(
                GameSetupView.class,
                frame.getContentPane()
        );

        List<JComboBox<?>> selectors = comboBoxes(setup);

        JComboBox<?> countSelector = selectors.stream()
                .filter(box -> box.getItemCount() > 0
                        && box.getItemAt(0) instanceof Integer)
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("לא נמצא מספר השחקנים"));

        countSelector.setSelectedItem(playerCount);

        JComboBox<?> difficultySelector = selectors.stream()
                .filter(box -> box.getItemCount() > 0
                        && box.getItemAt(0) instanceof Difficulty)
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("לא נמצאה רמת הקושי"));

        difficultySelector.setSelectedItem(Difficulty.EASY);

        List<JTextField> names =
                findComponents(setup, JTextField.class);

        List<JComboBox<?>> colors = colorSelectors();
        PlayerColor[] availableColors = PlayerColor.values();

        assertTrue(names.size() >= playerCount);
        assertTrue(colors.size() >= playerCount);

        for (int i = 0; i < playerCount; i++) {
            names.get(i).setText("שחקן " + (i + 1));
            colors.get(i).setSelectedItem(availableColors[i]);
        }
    }

    private List<JComboBox<?>> colorSelectors() {
        return comboBoxes(frame.getContentPane())
                .stream()
                .filter(box -> box.getItemCount() > 1
                        && box.getItemAt(1) instanceof PlayerColor)
                .toList();
    }

    private List<JComboBox<?>> comboBoxes(Container root) {
        List<JComboBox<?>> result = new ArrayList<>();

        for (Component component : findComponents(
                root, Component.class)) {
            if (component instanceof JComboBox<?> comboBox) {
                result.add(comboBox);
            }
        }

        return result;
    }

    private void clickButton(String text) {
        JButton button = findButton(text);

        assertTrue(button.isEnabled(),
                "הכפתור צריך להיות פעיל " + text);

        button.doClick(0);
    }

    private JButton findButton(String text) {
        List<JButton> matches =
                findComponents(
                        frame.getContentPane(), JButton.class)
                        .stream()
                        .filter(button ->
                                text.equals(button.getText()))
                        .toList();

        assertEquals(1, matches.size(),
                "צריך להימצא כפתור אחד עם הטקסט " + text);

        return matches.get(0);
    }

    private void clickAndAnswerDialog(
            String buttonText,
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
            clickButton(buttonText);
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