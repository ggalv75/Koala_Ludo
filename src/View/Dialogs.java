package View;

import javax.swing.*;
import java.awt.*;

/** All message boxes of the application (dialogs are part of the View, not the Controller). */
public final class Dialogs {
    private Dialogs() { }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, rtl(message), "שגיאה", JOptionPane.ERROR_MESSAGE);
    }

    public static void warning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, rtl(message), "בדיקת קלט", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, rtl(message), title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, rtl(message), title, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    /** Rules of the Easy level, as implemented in iteration 1. */
    public static void rules(Component parent) {
        String text = String.join("\n",
                "• 2-4 שחקנים, לכל שחקן ארבעה כלים שמתחילים בטירה.",
                "• בכל תור מטילים קובייה רגילה (1-6) ובוחרים כלי מסומן להזזה.",
                "• כלי יוצא מהטירה למשבצת ההתחלה (#) רק בתוצאה 6.",
                "• 6 מזכה בהטלה נוספת. השישייה השלישית ברצף מתבטלת והתור עובר.",
                "• כלי שנוחת על כלי יחיד של יריב במשבצת לא מוגנת מחזיר אותו לטירה.",
                "• משבצות התחלה (#), הגנה (★) ומסלול הבית מוגנות מאכילה.",
                "• שני כלים או יותר של אותו שחקן במשבצת יוצרים חסימה (Double Block):",
                "  יריב לא יכול לעבור דרכה או לנחות עליה.",
                "• כלי מגיע לחץ בצבע שלו ונכנס למסלול הבית. הכניסה לסיום דורשת הטלה מדויקת.",
                "• מי שמכניס את ארבעת הכלים מקבל את המקום הבא. המשחק נגמר כשנשאר שחקן אחד.");
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(Theme.font(15, false));
        area.setBackground(Theme.SURFACE);
        area.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JOptionPane.showMessageDialog(parent, area, "חוקי המשחק - רמה קלה", JOptionPane.INFORMATION_MESSAGE);
    }

    private static JLabel rtl(String message) {
        JLabel label = new JLabel("<html><div dir='rtl' style='width:320px'>"
                + escape(message).replace("\n", "<br>") + "</div></html>");
        label.setFont(Theme.font(15, false));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return label;
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
