package View;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class MainMenuViewTest {

    @Test
    void newGameButtonInvokesOnlyNewGameAction() throws Exception {
        assertNavigationAction("משחק חדש", 0);
    }

    @Test
    void rulesButtonInvokesOnlyRulesAction() throws Exception {
        assertNavigationAction("חוקי המשחק", 1);
    }

    @Test
    void exitButtonInvokesOnlyExitAction() throws Exception {
        assertNavigationAction("יציאה", 2);
    }

    @Test
    void historyButtonIsDisabledInIterationOne() throws Exception {
        assertUnavailableButton("היסטוריית משחקים (באיטרציה 3)");
    }

    @Test
    void questionManagementButtonIsDisabledInIterationOne()
            throws Exception {
        assertUnavailableButton("ניהול שאלות (באיטרציה 3)");
    }

    private void assertNavigationAction(
            String buttonText, int expectedAction) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            int[] calls = new int[3];

            MainMenuView menu = new MainMenuView(
                    () -> calls[0]++,
                    () -> calls[1]++,
                    () -> calls[2]++
            );

            JButton button = findButton(menu, buttonText);

            assertTrue(button.isEnabled(),
                    "כפתור הניווט צריך להיות פעיל");

            assertArrayEquals(new int[] {0, 0, 0}, calls,
                    "יצירת המסך לא אמורה להפעיל פעולת ניווט");

            button.doClick(0);

            int[] expectedCalls = new int[3];
            expectedCalls[expectedAction] = 1;

            assertArrayEquals(expectedCalls, calls,
                    "לחיצה צריכה להפעיל פעם אחת רק את הפעולה המתאימה");
        });
    }

    private void assertUnavailableButton(String buttonText)
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            int[] calls = new int[3];

            MainMenuView menu = new MainMenuView(
                    () -> calls[0]++,
                    () -> calls[1]++,
                    () -> calls[2]++
            );

            JButton button = findButton(menu, buttonText);

            assertFalse(button.isEnabled(),
                    "הכפתור צריך להיות מושבת באיטרציה 1");

            button.doClick(0);

            assertArrayEquals(new int[] {0, 0, 0}, calls,
                    "כפתור מושבת לא אמור להפעיל פעולת ניווט");
        });
    }

    private JButton findButton(Container container, String text) {
        JButton button = searchButton(container, text);

        assertNotNull(button, "לא נמצא כפתור עם הטקסט " + text);

        return button;
    }

    private JButton searchButton(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button
                    && text.equals(button.getText())) {
                return button;
            }

            if (component instanceof Container childContainer) {
                JButton found = searchButton(childContainer, text);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}