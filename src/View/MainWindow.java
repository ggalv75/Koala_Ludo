package View;

import javax.swing.*;
import java.awt.*;

/** The application window. The Controller tells it which screen to show. */
public final class MainWindow {
    private final JFrame frame = new JFrame("לודו קואלה");

    /** Configure the desktop window and its initial dimensions. */
    public MainWindow() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1220, 840);
        frame.setMinimumSize(new Dimension(1000, 720));
        frame.setLocationRelativeTo(null);
    }

    /** Replace the visible screen while preserving right-to-left layout. */
    public void showScreen(JComponent screen) {
        screen.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        frame.setContentPane(screen);
        frame.revalidate();
        frame.repaint();
        if (!frame.isVisible()) frame.setVisible(true);
    }

    /** Parent component for dialogs. */
    public Component component() { return frame; }

    /** Dispose the application window. */
    public void close() { frame.dispose(); }
}
