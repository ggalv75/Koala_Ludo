package Controller;

import View.Dialogs;
import View.SoundManager;

import javax.swing.*;

/** Starts the application on the Swing event thread. */
public final class LudoApplication {
    /** Prevent construction of the application entry-point utility. */
    private LudoApplication() { }

    /** Launch the application safely on the Swing event dispatch thread. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // The cross-platform look keeps colours and Hebrew layout the same on every computer.
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                // The default look and feel is fine as well.
            }
            try {
                new GameController(new SoundManager()).start();
            } catch (RuntimeException error) {
                error.printStackTrace();
                Dialogs.error(null, "לא ניתן להפעיל את המשחק: " + error.getMessage());
            }
        });
    }
}
