package View;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A small on/off button for the music or the sound effects.
 * The current state is always read from {@link GameSounds}, so every screen
 * shows the same setting without keeping its own copy.
 */
public final class SoundToggle extends JToggleButton {
    private static final long serialVersionUID = 1L;

    /** What the button controls; decides its icon, label and tooltip. */
    public enum Kind { MUSIC, EFFECTS }

    private final Kind kind;

    /** Create the button for one switch of the given sound system. */
    public SoundToggle(Kind kind, GameSounds sounds) {
        this.kind = kind;
        BooleanSupplier isOn = kind == Kind.MUSIC ? sounds::isMusicOn : sounds::isEffectsOn;
        Consumer<Boolean> setOn = kind == Kind.MUSIC ? sounds::setMusicOn : sounds::setEffectsOn;
        setName(kind == Kind.MUSIC ? "music-toggle" : "effects-toggle");
        setSelected(isOn.getAsBoolean());
        setFont(Theme.font(13, true));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIconTextGap(6);
        setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        addActionListener(e -> {
            setOn.accept(isSelected());
            refresh();
        });
        refresh();
    }

    private void refresh() {
        boolean on = isSelected();
        String subject = kind == Kind.MUSIC ? "מוזיקה" : "צלילים";
        setText(subject);
        setToolTipText((on ? "כיבוי " : "הפעלת ") + (kind == Kind.MUSIC ? "מוזיקת הרקע" : "צלילי המשחק"));
        getAccessibleContext().setAccessibleName(subject + (on ? " פועלת" : " כבויה"));
        setForeground(on ? Theme.KOALA_PRIMARY_DARK : Theme.KOALA_MUTED);
        setIcon(new SymbolIcon(kind, on));
        repaint();
    }

    /** Rounded pill background: tinted when on, plain with a grey outline when off. */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = getHeight();
        boolean on = isSelected();
        Color fill = on ? Theme.KOALA_BACKGROUND : Color.WHITE;
        if (getModel().isRollover()) fill = on ? new Color(222, 238, 229) : new Color(246, 249, 247);
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g.setColor(on ? Theme.KOALA_PRIMARY : Theme.KOALA_BORDER);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g.dispose();
        super.paintComponent(graphics);
    }

    /** Draws a music note or a speaker; a diagonal stroke marks "off". */
    private static final class SymbolIcon implements Icon {
        private static final int SIZE = 16;
        private final Kind kind;
        private final boolean on;

        SymbolIcon(Kind kind, boolean on) {
            this.kind = kind;
            this.on = on;
        }

        @Override public int getIconWidth() { return SIZE; }

        @Override public int getIconHeight() { return SIZE; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            g.setColor(on ? Theme.KOALA_PRIMARY : Theme.KOALA_MUTED);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (kind == Kind.MUSIC) paintNote(g); else paintSpeaker(g);
            if (!on) {
                g.setColor(Theme.KOALA_ERROR);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(2, 2, SIZE - 2, SIZE - 2);
            }
            g.dispose();
        }

        private static void paintNote(Graphics2D g) {
            g.fillOval(1, 10, 6, 5);
            g.fillOval(9, 8, 6, 5);
            g.drawLine(6, 12, 6, 3);
            g.drawLine(14, 10, 14, 1);
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(6, 3, 14, 1);
        }

        private static void paintSpeaker(Graphics2D g) {
            Path2D.Double body = new Path2D.Double();
            body.moveTo(1, 6);
            body.lineTo(4, 6);
            body.lineTo(8, 2);
            body.lineTo(8, 14);
            body.lineTo(4, 10);
            body.lineTo(1, 10);
            body.closePath();
            g.fill(body);
            g.drawArc(7, 5, 5, 6, -55, 110);
            g.drawArc(7, 2, 8, 12, -55, 110);
        }
    }
}
