package View;

import javax.swing.*;
import java.awt.*;

/** Draws the regular die with pips (FR-15). Value 0 shows an empty die. */
public final class DieFace extends JComponent {
    private static final long serialVersionUID = 1L;
    private int value;

    /** Initialize a square visual representation of the step die. */
    public DieFace() { setPreferredSize(new Dimension(76, 76)); }

    /** Update the displayed die face and request a repaint. */
    public void setValue(int value) {
        this.value = value;
        repaint();
    }

    /** Draw the die background, outline and visible pips. */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = Math.min(getWidth(), getHeight()) - 6;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, size, size, size / 4, size / 4);
        g.setColor(value == 0 ? Theme.BORDER : Theme.INK);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, size, size, size / 4, size / 4);
        int pip = Math.max(6, size / 6);
        int[][] spots = switch (value) {
            case 1 -> new int[][]{{1, 1}};
            case 2 -> new int[][]{{0, 0}, {2, 2}};
            case 3 -> new int[][]{{0, 0}, {1, 1}, {2, 2}};
            case 4 -> new int[][]{{0, 0}, {0, 2}, {2, 0}, {2, 2}};
            case 5 -> new int[][]{{0, 0}, {0, 2}, {1, 1}, {2, 0}, {2, 2}};
            case 6 -> new int[][]{{0, 0}, {0, 2}, {1, 0}, {1, 2}, {2, 0}, {2, 2}};
            default -> new int[0][];
        };
        for (int[] s : spots) {
            int px = x + size / 4 * (s[1] + 1) - pip / 2;
            int py = y + size / 4 * (s[0] + 1) - pip / 2;
            g.fillOval(px, py, pip, pip);
        }
        g.dispose();
    }
}
