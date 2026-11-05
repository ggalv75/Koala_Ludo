package View;

import Model.PlayerColor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Shared colours, fonts and small widget factories, so every screen looks the same. */
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

    private static final String FONT_FAMILY = Font.DIALOG;

    private Theme() { }

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

    public static Color softColorOf(PlayerColor color) {
        return switch (color) {
            case RED -> new Color(253, 232, 230);
            case GREEN -> new Color(230, 244, 231);
            case YELLOW -> new Color(255, 249, 219);
            case BLUE -> new Color(225, 240, 253);
        };
    }

	/**
	 * Stronger colours for home-path squares. Keeps home paths visually distinct
	 * from regular cells.
	 */
	public static Color homePathColorOf(PlayerColor color) {
		return switch (color) {
		case RED -> new Color(248, 159, 154);
		case GREEN -> new Color(153, 213, 157);
		case YELLOW -> new Color(255, 221, 119);
		case BLUE -> new Color(151, 203, 248);
		};
	}


    public static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(INK);
        return label;
    }

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
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(colorOf(color));
                g2.fillOval(x, y, diameter, diameter);
                g2.dispose();
            }
            @Override public int getIconWidth() { return diameter; }
            @Override public int getIconHeight() { return diameter; }
        };
    }
}
