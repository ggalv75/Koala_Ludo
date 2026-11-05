package Controller;

import View.Dialogs;

import javax.swing.*;

/** Starts the application on the Swing event thread. */
public final class LudoApplication {
    private LudoApplication() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // The cross-platform look keeps colours and Hebrew layout the same on every computer.
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                // The default look and feel is fine as well.
            }
            try {
                new GameController().start();
            } catch (RuntimeException error) {
                error.printStackTrace();
                Dialogs.error(null, "לא ניתן להפעיל את המשחק: " + error.getMessage());
            }
        });
    }
}
