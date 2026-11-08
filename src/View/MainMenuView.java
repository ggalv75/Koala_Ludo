
package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Main menu of Koala Ludo.
 * Visual presentation only; navigation is delegated to the Controller.
 */
public final class MainMenuView extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final Color GREEN = Theme.KOALA_PRIMARY;
    private static final Color GREEN_DARK = Theme.KOALA_PRIMARY_DARK;
    private static final Color DARK = Theme.KOALA_TEXT;
    private static final Color PALE = Theme.KOALA_BACKGROUND;
    private static final Color MUTED = Theme.KOALA_MUTED;

    public MainMenuView(
            Runnable onNewGame,
            Runnable onRules,
            Runnable onExit) {

        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(PALE);

        // Main centered card with a soft shadow.
        JPanel card = new JPanel(new BorderLayout(0, 18)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Soft drop shadow.
                g2.setColor(new Color(30, 72, 58, 18));
                g2.fillRoundRect(9, 12, w - 18, h - 17, 34, 34);

                g2.setColor(new Color(30, 72, 58, 12));
                g2.fillRoundRect(6, 7, w - 12, h - 15, 34, 34);

                // Main card surface.
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(4, 3, w - 8, h - 15, 34, 34);

                // Subtle outline.
                g2.setColor(new Color(223, 235, 227));
                g2.drawRoundRect(4, 3, w - 9, h - 16, 34, 34);

                g2.dispose();

                super.paintComponent(g);
            }
        };

        card.setOpaque(false);
        card.setPreferredSize(new Dimension(545, 690));
        card.setBorder(new EmptyBorder(18, 48, 32, 48));

        // Header: Koala mascot, title and subtitle.
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        KoalaLogo logo = new KoalaLogo();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("KOALA LUDO");
        title.setFont(
                new Font(Font.DIALOG, Font.BOLD, 42));
        title.setForeground(DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("PLAY  \u2022  ROLL  \u2022  WIN");
        subtitle.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(logo);
        header.add(Box.createVerticalStrut(2));
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);

        card.add(header, BorderLayout.NORTH);

        // Navigation buttons.
        // Original Controller callbacks are preserved.
        JPanel actions = new JPanel(new GridLayout(5, 1, 0, 12));
        actions.setOpaque(false);

        JButton newGame = new MenuButton("משחק חדש", true);

        JButton rules = new MenuButton("חוקי המשחק", false);

        JButton history = new MenuButton("היסטוריית משחקים  \u2022  בקרוב", false);

        JButton questions = new MenuButton("ניהול שאלות  \u2022  בקרוב", false);

        JButton exit = new MenuButton("יציאה", false);

        // Stable identifiers for tests, independent of the displayed text.
        newGame.setName("menu-new-game");
        rules.setName("menu-rules");
        history.setName("menu-history");
        questions.setName("menu-questions");
        exit.setName("menu-exit");

        // Planned for iteration 3.
        history.setEnabled(false);
        questions.setEnabled(false);

        newGame.addActionListener(e -> onNewGame.run());

        rules.addActionListener(e -> onRules.run());

        exit.addActionListener(e -> onExit.run());

        actions.add(newGame);
        actions.add(rules);
        actions.add(history);
        actions.add(questions);
        actions.add(exit);

        card.add(actions, BorderLayout.CENTER);

        // Footer
        JLabel footer = new JLabel("DEVELOPED BY TEAM KOALA", SwingConstants.CENTER);

        footer.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        footer.setForeground(MUTED);

        card.add(footer, BorderLayout.SOUTH);

        add(card);
    }

    // Background: gradient and decorative circles.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2.setPaint(Theme.koalaBackgroundGradient(w, h));

        g2.fillRect(0, 0, w, h);

        g2.setColor(new Color(107, 170, 130, 28));
        g2.fillOval(-140, -130, 430, 430);
        g2.fillOval(w - 290, h - 290, 430, 430);

        g2.setColor(new Color(255, 255, 255, 75));
        g2.fillOval(w - 240, -100, 310, 310);
        g2.fillOval(-140, h - 200, 280, 280);

        g2.dispose();
    }

    // Custom rounded button with hover and pressed states.
    private static final class MenuButton extends JButton {

        private static final long serialVersionUID = 1L;

        private final boolean primary;
        private boolean hovered;

        MenuButton(String text, boolean primary) {
            super(text);

            this.primary = primary;

            setFont(new Font(Font.DIALOG, Font.BOLD, 16));

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);

            setBorder(new EmptyBorder(12, 20, 12, 20));

            setForeground(primary ? Color.WHITE : DARK);

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill;

            if (!isEnabled()) {
                fill = new Color(244, 247, 245);
                setForeground(new Color(158, 171, 163));

            } else if (primary) {
                fill = getModel().isPressed() ? GREEN_DARK : hovered ? new Color(47, 130, 99) : GREEN;

                setForeground(Color.WHITE);

            } else {
                fill = getModel().isPressed() ? new Color(207, 228, 216) : hovered ? new Color(220, 239, 228) : PALE;

                setForeground(DARK);
            }

            int w = getWidth();
            int h = getHeight();

            g2.setColor(fill);
            g2.fillRoundRect(1, 1, w - 2, h - 2, 20, 20);

            if (isEnabled() && hovered) {
                g2.setColor(new Color(31, 112, 78, 45));
                g2.setStroke(new BasicStroke(1.5f));

                g2.drawRoundRect(1, 1, w - 3, h - 3, 20, 20);
            }

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // Koala mascot with two properly drawn dice.
    private static final class KoalaLogo extends JComponent {

        private static final long serialVersionUID = 1L;

        KoalaLogo() {
            Dimension size = new Dimension(245, 195);

            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Scale the whole mascot and its dice together.
            g2.translate(0, 10);
            g2.scale(1.25, 1.25);

            // Soft circular background.
            g2.setColor(new Color(225, 242, 232));
            g2.fillOval(26, 6, 118, 118);

            // Outer ears.
            g2.setColor(new Color(151, 168, 177));
            g2.fillOval(36, 26, 42, 42);
            g2.fillOval(92, 26, 42, 42);

            // Inner ears.
            g2.setColor(new Color(235, 182, 193));
            g2.fillOval(46, 36, 22, 22);
            g2.fillOval(102, 36, 22, 22);

            // Koala face.
            g2.setColor(new Color(179, 193, 201));
            g2.fillOval(45, 20, 72, 74);

            // Eyes.
            g2.setColor(new Color(58, 78, 88));
            g2.fillOval(64, 52, 7, 9);
            g2.fillOval(91, 52, 7, 9);

            // Eye highlights.
            g2.setColor(Color.WHITE);
            g2.fillOval(66, 54, 2, 2);
            g2.fillOval(93, 54, 2, 2);

            // Eyebrows.
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(74, 93, 102));

            g2.drawArc(60, 45, 14, 8, 0, 180);
            g2.drawArc(87, 45, 14, 8, 0, 180);

            // Cheeks.
            g2.setColor(new Color(231, 167, 179, 150));
            g2.fillOval(55, 71, 14, 8);
            g2.fillOval(94, 71, 14, 8);

            // Nose.
            g2.setColor(new Color(50, 67, 75));
            g2.fillOval(75, 58, 18, 29);

            // Smile.
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawArc(73, 80, 22, 12, 190, 160);

            // Two dice, placed beside the face.
            // Left die: 2 pips.
            drawDie(g2, 120, 104, 2, -10);

            // Right die: 3 pips.
            drawDie(g2, 153, 104, 3, 10);

            g2.dispose();
        }

        /**
         * Draw one die with rotation, shadow,
         * a rounded border and symmetrical pips.
         */
        private void drawDie(Graphics2D g, int centerX, int centerY, int face, double angleDegrees) {

            Graphics2D d = (Graphics2D) g.create();

            d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            d.translate(centerX, centerY);
            d.rotate(Math.toRadians(angleDegrees));

            // Shadow.
            d.setColor(new Color(37, 111, 86, 35));
            d.fillRoundRect(-14, -12, 32, 32, 8, 8);

            // White body.
            d.setColor(Color.WHITE);
            d.fillRoundRect(-16, -16, 32, 32, 8, 8);

            // Green outline.
            d.setColor(GREEN);
            d.setStroke(new BasicStroke(2f));
            d.drawRoundRect(-16, -16, 32, 32, 8, 8);

            drawPips(d, face);

            d.dispose();
        }

        /**
         * Draw correctly positioned dice pips.
         */
        private void drawPips(Graphics2D g, int face) {
            switch (face) {
                case 2 -> {
                    pip(g, -7, -7);
                    pip(g, 7, 7);
                }

                case 3 -> {
                    pip(g, -7, -7);
                    pip(g, 0, 0);
                    pip(g, 7, 7);
                }

                case 4 -> {
                    pip(g, -7, -7);
                    pip(g, 7, -7);
                    pip(g, -7, 7);
                    pip(g, 7, 7);
                }

                default -> {
                    pip(g, 0, 0);
                }
            }
        }

        /**
         * Draw one circular pip centered at (x, y).
         */
        private void pip(Graphics2D g, int x, int y) {
            g.setColor(GREEN);
            g.fillOval(
                    x - 3,
                    y - 3,
                    6,
                    6);
        }
    }
}
