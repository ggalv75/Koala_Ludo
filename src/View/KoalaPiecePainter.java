package View;

import Model.PlayerColor;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.Objects;

/** Draws consistent, scalable koala faces without introducing assets or game rules into the View. */
public final class KoalaPiecePainter {
    private static final Color OUTLINE = new Color(34, 45, 62);
    private static final Color NOSE = new Color(45, 52, 65);
    private static final Color EYE_HIGHLIGHT = Color.WHITE;
    private static final Color MUZZLE = new Color(255, 249, 237);

    /** Prevent instantiation of the stateless drawing utility. */
    private KoalaPiecePainter() { }

    /** Paint one numbered koala piece at any board position, including animated positions. */
    public static void draw(Graphics2D graphics, PlayerColor color, int cx, int cy, int radius, int number) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(color, "color");
        if (radius <= 0) return;

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(cx, cy);
            g.scale(radius, radius);
            g.setStroke(new BasicStroke(0.075f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Color base = Theme.colorOf(color);
            Color earInner = mix(base, new Color(255, 226, 229), 0.64f);
            Color headHighlight = mix(base, Color.WHITE, 0.21f);
            Color headShade = mix(base, OUTLINE, 0.12f);

            // Shadows and ears sit behind the face and keep the silhouette recognizable at small sizes.
            g.setColor(new Color(20, 29, 43, 48));
            g.fill(new Ellipse2D.Double(-0.80, -0.70, 1.65, 1.54));
            paintEllipse(g, -0.99, -0.72, 0.69, 0.78, headShade, OUTLINE);
            paintEllipse(g, 0.30, -0.72, 0.69, 0.78, headShade, OUTLINE);
            if (radius >= 8) {
                paintEllipse(g, -0.82, -0.55, 0.37, 0.42, earInner, null);
                paintEllipse(g, 0.45, -0.55, 0.37, 0.42, earInner, null);
            }

            paintEllipse(g, -0.75, -0.78, 1.50, 1.52, base, OUTLINE);
            g.setColor(headHighlight);
            g.fill(new Ellipse2D.Double(-0.49, -0.65, 0.74, 0.26));
            if (radius >= 10) {
                paintEllipse(g, -0.52, 0.06, 1.04, 0.51, MUZZLE, null);
                paintEllipse(g, -0.57, -0.14, 0.24, 0.17, headHighlight, null);
                paintEllipse(g, 0.33, -0.14, 0.24, 0.17, headHighlight, null);
            }

            paintEllipse(g, -0.39, -0.27, 0.16, 0.20, OUTLINE, null);
            paintEllipse(g, 0.23, -0.27, 0.16, 0.20, OUTLINE, null);
            if (radius >= 12) {
                paintEllipse(g, -0.35, -0.23, 0.060, 0.060, EYE_HIGHLIGHT, null);
                paintEllipse(g, 0.27, -0.23, 0.060, 0.060, EYE_HIGHLIGHT, null);
            }
            paintEllipse(g, -0.22, -0.11, 0.44, 0.49, NOSE, null);
            if (radius >= 13) paintEllipse(g, -0.14, -0.045, 0.14, 0.10, new Color(122, 130, 143), null);

        } finally {
            g.dispose();
        }
        if (radius >= 11 && number > 0) drawNumberBadge(graphics, cx, cy, radius, number);
    }

    /** Provide the same koala silhouette for player labels and results icons. */
    public static Icon icon(PlayerColor color, int size) {
        Objects.requireNonNull(color, "color");
        if (size <= 0) throw new IllegalArgumentException("Icon size must be positive");
        return new Icon() {
            /** Draw a centered koala in its allotted icon bounds. */
            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                if (!(graphics instanceof Graphics2D g)) return;
                draw(g, color, x + size / 2, y + size / 2, Math.max(3, (int) (size * 0.43)), 0);
            }

            /** Report the icon width requested by the caller. */
            @Override public int getIconWidth() { return size; }

            /** Report the icon height requested by the caller. */
            @Override public int getIconHeight() { return size; }
        };
    }

    /** Draw a colored ellipse and an optional clear outline in normalized coordinates. */
    private static void paintEllipse(Graphics2D g, double x, double y, double width, double height,
                                     Color fill, Color outline) {
        Ellipse2D shape = new Ellipse2D.Double(x, y, width, height);
        g.setColor(fill);
        g.fill(shape);
        if (outline != null) {
            g.setColor(outline);
            g.draw(shape);
        }
    }

    /** Preserve each piece's number in a crisp, high-contrast pixel-space badge. */
    private static void drawNumberBadge(Graphics2D graphics, int cx, int cy, int radius, int number) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int x = cx + Math.round(radius * 0.64f);
            int y = cy + Math.round(radius * 0.64f);
            int badgeRadius = Math.max(4, Math.round(radius * 0.26f));
            g.setColor(Color.WHITE);
            g.fillOval(x - badgeRadius, y - badgeRadius, badgeRadius * 2, badgeRadius * 2);
            g.setColor(OUTLINE);
            g.setStroke(new BasicStroke(Math.max(1.15f, radius * 0.047f)));
            g.drawOval(x - badgeRadius, y - badgeRadius, badgeRadius * 2, badgeRadius * 2);
            g.setFont(new Font(Font.DIALOG, Font.BOLD, Math.max(7, Math.round(radius * 0.42f))));
            String text = Integer.toString(number);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, x - metrics.stringWidth(text) / 2,
                    y + (metrics.getAscent() - metrics.getDescent()) / 2);
        } finally {
            g.dispose();
        }
    }

    /** Mix two colors without changing the player's official theme color. */
    private static Color mix(Color a, Color b, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        return new Color(Math.round(a.getRed() * (1 - t) + b.getRed() * t),
                Math.round(a.getGreen() * (1 - t) + b.getGreen() * t),
                Math.round(a.getBlue() * (1 - t) + b.getBlue() * t));
    }
}
