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
        assertNavigationAction("menu-new-game", 0);
    }

    @Test
    void rulesButtonInvokesOnlyRulesAction() throws Exception {
        assertNavigationAction("menu-rules", 1);
    }

    @Test
    void exitButtonInvokesOnlyExitAction() throws Exception {
        assertNavigationAction("menu-exit", 2);
    }

    @Test
    void historyButtonIsDisabledInIterationOne() throws Exception {
        assertUnavailableButton("menu-history");
    }

    @Test
    void questionManagementButtonIsDisabledInIterationOne()
            throws Exception {
        assertUnavailableButton("menu-questions");
    }

    private void assertNavigationAction(
            String buttonName, int expectedAction) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            int[] calls = new int[3];

            MainMenuView menu = new MainMenuView(
                    () -> calls[0]++,
                    () -> calls[1]++,
                    () -> calls[2]++
            );

            JButton button = findButton(menu, buttonName);

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

    private void assertUnavailableButton(String buttonName)
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            int[] calls = new int[3];

            MainMenuView menu = new MainMenuView(
                    () -> calls[0]++,
                    () -> calls[1]++,
                    () -> calls[2]++
            );

            JButton button = findButton(menu, buttonName);

            assertFalse(button.isEnabled(),
                    "הכפתור צריך להיות מושבת באיטרציה 1");

            button.doClick(0);

            assertArrayEquals(new int[] {0, 0, 0}, calls,
                    "כפתור מושבת לא אמור להפעיל פעולת ניווט");
        });
    }

    private JButton findButton(Container container, String name) {
        JButton button = searchButton(container, name);

        assertNotNull(button, "לא נמצא כפתור עם המזהה " + name);

        return button;
    }

    private JButton searchButton(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button
                    && name.equals(button.getName())) {
                return button;
            }

            if (component instanceof Container childContainer) {
                JButton found = searchButton(childContainer, name);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}