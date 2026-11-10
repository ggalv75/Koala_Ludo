package View;

import Controller.GameController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Real graphical smoke test. Run with desktop/Xvfb, not -Djava.awt.headless=true. */
public final class LudoGuiSmokeTest {
    private static JFrame frame;

    /** Assert the expected GUI interaction succeeded. */
    private static void expect(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** Recursively find Swing components of the requested type. */
    private static <T extends Component> List<T> descendants(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) result.add(type.cast(component));
            if (component instanceof Container c) result.addAll(descendants(c, type));
        }
        return result;
    }

    /** Simulate a user click in the visible test interface. */
    private static void click(String title) {
        JButton button = descendants(frame.getContentPane(), JButton.class).stream()
                .filter(b -> title.equals(b.getText())).findFirst().orElseThrow();
        button.doClick();
    }

    /** Run the test cases and report successful assertions. */
    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                new GameController().start();
                for (Window window : Window.getWindows()) {
                    if (window instanceof JFrame f && "לודו קואלה".equals(f.getTitle())) frame = f;
                }
                expect(frame != null && frame.isVisible(), "main window should be visible");
                click("משחק חדש");
                expect(frame.getContentPane() instanceof GameSetupView, "setup view missing");
                List<JTextField> fields = descendants(frame.getContentPane(), JTextField.class);
                expect(fields.size() == 4, "four name fields expected");
                fields.get(0).setText("Alice");
                fields.get(1).setText("Bob");
                List<AbstractButton> colorButtons = descendants(frame.getContentPane(), AbstractButton.class);
                AbstractButton first = colorButtons.stream()
                        .filter(b -> "player-color-0-RED".equals(b.getName()))
                        .findFirst().orElseThrow();
                AbstractButton second = colorButtons.stream()
                        .filter(b -> "player-color-1-GREEN".equals(b.getName()))
                        .findFirst().orElseThrow();
                expect(first.isEnabled() && second.isEnabled(), "colors must be selectable");
                first.doClick();
                second.doClick();
                expect(first.isSelected() && second.isSelected(), "two unique player colors selected");
                click("התחלת משחק");
                expect(frame.getContentPane() instanceof GameBoardView, "game board missing");
                expect(descendants(frame.getContentPane(), BoardCanvas.class).size() == 1,
                        "animated board component missing");
                System.out.println("PASS: actual Swing UI opened, setup completed, board displayed");
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    if (frame.getContentPane() instanceof GameBoardView view) view.stopClock();
                    frame.dispose();
                }
            });
        }
    }
}
