package View;

import Model.PlayerColor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Shared colors, fonts and small widget factories, so every screen looks the same. */
public final class Theme {
    public static final Color BACKGROUND = new Color(246, 247, 251);
    public static final Color SURFACE = Color.WHITE;
    public static final Color INK = new Color(35, 47, 67);
    public static final Color MUTED = new Color(98, 111, 130);
    public static final Color PRIMARY = new Color(79, 70, 229);
    public static final Color BORDER = new Color(218, 224, 234);
    public static final Color SAFE = new Color(29, 130, 87);
    public static final Color SAFE_SOFT = new Color(220, 245, 235);
    public static final Color HIGHLIGHT = new Color(251, 191, 36);

    // Shared Koala UI color palette.
    public static final Color KOALA_PRIMARY = new Color(37, 111, 86);
    public static final Color KOALA_PRIMARY_DARK = new Color(24, 85, 65);
    public static final Color KOALA_TEXT = new Color(35, 63, 55);
    public static final Color KOALA_MUTED = new Color(104, 131, 119);
    public static final Color KOALA_BACKGROUND = new Color(235, 244, 239);
    public static final Color KOALA_BORDER = new Color(216, 231, 222);
    public static final Color KOALA_ROW_BACKGROUND = new Color(246, 249, 247);
    public static final Color KOALA_ERROR = new Color(175, 49, 49);

    private static final Color KOALA_GRADIENT_START = new Color(235, 247, 239);
    private static final Color KOALA_GRADIENT_END = new Color(216, 234, 236);

    private static final String FONT_FAMILY = Font.DIALOG;

    /** Prevent instantiation of the shared Swing theme. */
    private Theme() { }
    

    /**
     * Creates the shared Koala background gradient.
     * Used by the main menu and game setup screens.
     */
    public static Paint koalaBackgroundGradient(int width, int height) {
        return new GradientPaint(0, 0, KOALA_GRADIENT_START, width, height, KOALA_GRADIENT_END);
    }

    /** Build a theme-consistent dialog font at the requested size. */
    public static Font font(int size, boolean bold) {
        return new Font(FONT_FAMILY, bold ? Font.BOLD : Font.PLAIN, size);
    }

    /** Screen colour of a player colour (kept in the View so the Model has no AWT dependency). */
    public static Color colorOf(PlayerColor color) {
        return switch (color) {
            case RED -> new Color(229, 57, 53);
            case GREEN -> new Color(67, 160, 71);
            case YELLOW -> new Color(240, 196, 25);
            case BLUE -> new Color(30, 136, 229);
        };
    }

    /** Return a lighter background shade for the player color. */
    public static Color softColorOf(PlayerColor color) {
        return switch (color) {
            case RED -> new Color(253, 232, 230);
            case GREEN -> new Color(230, 244, 231);
            case YELLOW -> new Color(255, 249, 219);
            case BLUE -> new Color(225, 240, 253);
        };
    }

    /** Create a theme-consistent text label. */
    public static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(INK);
        return label;
    }

    /** Create a styled primary or secondary action button. */
    public static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(font(15, true));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(primary ? PRIMARY : SURFACE);
        button.setForeground(primary ? Color.WHITE : INK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? PRIMARY : BORDER),
                new EmptyBorder(10, 20, 10, 20)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** White card with a thin border. */
    public static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 16, 14, 16)));
        return panel;
    }

    /** Full-screen page background. */
    public static JPanel page(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));
        return panel;
    }

    /** Create a theme-consistent player name field. */
    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setFont(font(15, false));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(6, 8, 6, 8)));
        return field;
    }

    /** Small filled circle used next to player names. */
    public static Icon colorDot(PlayerColor color, int diameter) {
        return new Icon() {
            /** Draw a filled circular marker in the player color. */
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(colorOf(color));
                g2.fillOval(x, y, diameter, diameter);
                g2.dispose();
            }
            /** Return the marker icon width. */
            @Override public int getIconWidth() { return diameter; }
            /** Return the marker icon height. */
            @Override public int getIconHeight() { return diameter; }
        };
    }
}
