package View;

import javax.swing.*;
import java.awt.*;

/** All message boxes of the application (dialogs are part of the View, not the Controller). */
public final class Dialogs {
    /** Prevent external construction of the dialog helper. */
    private Dialogs() { }

    /** Show a right-to-left error dialog with the supplied message. */
    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, rtl(message), "שגיאה", JOptionPane.ERROR_MESSAGE);
    }

    /** Show a right-to-left validation warning dialog. */
    public static void warning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, rtl(message), "בדיקת קלט", JOptionPane.WARNING_MESSAGE);
    }

    /** Show an informational dialog with the supplied title. */
    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, rtl(message), title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Ask the user to confirm a potentially destructive action. */
    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, rtl(message), title, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    /** Display the Easy-level rules using the custom Koala dialog. */
    public static void rules(Component parent) {
        RulesDialog.show(parent);
    }
    
    /** Build an escaped and right-to-left message component. */
    private static JLabel rtl(String message) {
        JLabel label = new JLabel("<html><div dir='rtl' style='width:320px'>"
                + escape(message).replace("\n", "<br>") + "</div></html>");
        label.setFont(Theme.font(15, false));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return label;
    }

    /** Escape untrusted text before rendering it as Swing HTML. */
    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
