package View;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.ThreadLocalRandom;

/** Paints a tumbling six-sided die without changing the Model's authoritative result. */
public final class DieFace extends JComponent {
    private static final long serialVersionUID = 1L;
    /** Also sets the length of the dice sound (SoundSynthesizer). */
    static final int DURATION_MS = 850;
    private static final int FRAME_MS = 25;
    private static final int FACE_CHANGE_MS = 76;

    private int value;
    private int visualValue;
    private boolean rolling;
    private double progress;
    private long startedAt;
    private int lastFaceIndex;
    private Timer animationTimer;
    private Runnable onRollComplete;

    /** Give the animated die a consistent size inside the game's sidebar. */
    public DieFace() {
        setPreferredSize(new Dimension(86, 86));
        setMinimumSize(new Dimension(86, 86));
        setOpaque(false);
    }

    /** Display a result from the Model, never an independently generated game result. */
    public void setValue(int value) {
        if (value < 0 || value > 6) throw new IllegalArgumentException("Die face must be 0..6");
        this.value = value;
        if (!rolling) visualValue = value;
        repaint();
    }

    /** Report whether a visual roll is still playing. */
    public boolean isRolling() { return rolling; }

    /** Show the authoritative result most recently supplied by the Model. */
    int value() { return value; }

    /** Animate the dice first, then request exactly one real Model roll. */
    public void animateRoll(Runnable rollAction) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Die animation must start on the Swing EDT");
        }
        if (rolling) return;
        if (rollAction == null) throw new IllegalArgumentException("rollAction is required");
        rolling = true;
        progress = 0;
        startedAt = System.nanoTime();
        lastFaceIndex = -1;
        onRollComplete = rollAction;
        visualValue = 1 + ThreadLocalRandom.current().nextInt(6);
        animationTimer = new Timer(FRAME_MS, event -> advanceFrame());
        animationTimer.setCoalesce(true);
        animationTimer.start();
        repaint();
    }

    /** Skip remaining frames and request the pending roll once, if still active. */
    public void finishRollAnimation() {
        if (!rolling) return;
        Runnable completion = onRollComplete;
        resetAnimation();
        if (completion != null) completion.run();
    }

    /** Cancel an unfinished roll without contacting the Model when leaving the game. */
    public void cancelRollAnimation() {
        if (!rolling) return;
        resetAnimation();
    }

    /** Recompute rotation, bounce and temporary faces on a Swing timer frame. */
    private void advanceFrame() {
        if (!rolling) return;
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        progress = Math.min(1, elapsedMs / (double) DURATION_MS);
        int faceIndex = (int) (elapsedMs / FACE_CHANGE_MS);
        if (faceIndex != lastFaceIndex) {
            int next;
            do {
                next = 1 + ThreadLocalRandom.current().nextInt(6);
            } while (next == visualValue);
            visualValue = next;
            lastFaceIndex = faceIndex;
        }
        if (progress >= 1) finishRollAnimation();
        else repaint();
    }

    /** Restore the idle die and release its animation timer and completion callback. */
    private void resetAnimation() {
        if (animationTimer != null) animationTimer.stop();
        animationTimer = null;
        onRollComplete = null;
        rolling = false;
        progress = 0;
        visualValue = value;
        repaint();
    }

    /** Render a bouncing, spinning die with a soft shadow and beveled edges. */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            int size = Math.max(24, Math.min(getWidth(), getHeight()) - 26);
            double damping = rolling ? (1 - progress) : 0;
            double bounce = rolling ? Math.abs(Math.sin(4 * Math.PI * progress)) * (9 * damping) : 0;
            double ease = rolling ? 1 - Math.pow(1 - progress, 3) : 0;
            double angle = rolling ? 4 * Math.PI * ease + Math.sin(12 * Math.PI * progress) * 0.13 * damping : 0;
            double squish = rolling ? 0.07 * damping * Math.sin(8 * Math.PI * progress) : 0;

            // The floor shadow is stationary while the die hops above it.
            g.setColor(new Color(28, 45, 68, rolling ? 37 : 24));
            g.fill(new Ellipse2D.Double(cx - size * .38, cy + size * .44,
                    size * .76, Math.max(3, size * .1 - bounce * .12)));

            AffineTransform original = g.getTransform();
            g.translate(cx, cy - bounce);
            g.rotate(angle);
            g.scale(1 + squish, 1 - squish);
            drawFace(g, size, rolling ? visualValue : value);
            g.setTransform(original);
        } finally {
            g.dispose();
        }
    }

    /** Draw the six-sided die face and its pips at the animated local origin. */
    private void drawFace(Graphics2D g, int size, int face) {
        double half = size / 2.0;
        double radius = Math.max(8, size * .20);
        RoundRectangle2D.Double back = new RoundRectangle2D.Double(
                -half, -half + 4, size, size, radius, radius);
        g.setColor(new Color(144, 166, 188));
        g.fill(back);
        RoundRectangle2D.Double front = new RoundRectangle2D.Double(
                -half, -half, size, size - 4, radius, radius);
        g.setPaint(new GradientPaint(0, (float) -half, Color.WHITE,
                (float) size, (float) half, new Color(226, 239, 252)));
        g.fill(front);
        g.setColor(face == 0 ? Theme.BORDER : new Color(71, 102, 134));
        g.setStroke(new BasicStroke(1.7f));
        g.draw(front);
        g.setColor(new Color(42, 72, 109));
        double pipRadius = size * .073;
        for (int[] spot : spots(face)) {
            double px = (spot[1] - 1) * size * .26;
            double py = (spot[0] - 1) * size * .26 - 2;
            g.fill(new Ellipse2D.Double(px - pipRadius, py - pipRadius,
                    2 * pipRadius, 2 * pipRadius));
        }
    }

    /** Return the exact pip layout for a standard die face. */
    private static int[][] spots(int face) {
        return switch (face) {
            case 1 -> new int[][]{{1, 1}};
            case 2 -> new int[][]{{0, 0}, {2, 2}};
            case 3 -> new int[][]{{0, 0}, {1, 1}, {2, 2}};
            case 4 -> new int[][]{{0, 0}, {0, 2}, {2, 0}, {2, 2}};
            case 5 -> new int[][]{{0, 0}, {0, 2}, {1, 1}, {2, 0}, {2, 2}};
            case 6 -> new int[][]{{0, 0}, {0, 2}, {1, 0}, {1, 2}, {2, 0}, {2, 2}};
            default -> new int[0][];
        };
    }
}
