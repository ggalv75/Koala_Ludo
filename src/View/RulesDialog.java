
package View;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class RulesDialog {
	private static final Color ACCENT = Theme.KOALA_PRIMARY;
	private static final Color ACCENT_HOVER = new Color(47, 130, 99);
	private static final Color SOFT_ACCENT = new Color(225, 243, 232);
	private static final Color TEXT = Theme.KOALA_PRIMARY_DARK;
	private static final Color MUTED = new Color(75, 98, 90);
	private static final Color BORDER = new Color(205, 225, 212);
	private static final Color PALE = Theme.KOALA_BACKGROUND;
	private static final Color CARD_SURFACE = new Color(251, 254, 252);
	private static final Color FRAME = new Color(43, 52, 48);

    // Easy-level game rules.
    private static final String[][] RULES = {
        {"מטרת המשחק", "המשחק מיועד עבור 2-4 שחקנים. לכל שחקן ארבעה כלים בצבע ייחודי. המטרה היא להכניס את כל הכלים לבית הסיום."},
        {"מהלך התור", "בכל תור מטילים קובייה רגילה ומזיזים כלי אחד לפי המספר שהתקבל."},
        {"יציאה מהטירה", "רק הטלת 6 מאפשרת להוציא כלי מהטירה. הטלת 6 מעניקה גם הטלה נוספת."},
        {"שלוש שישיות ברצף", "אם מתקבל 6 שלוש פעמים ברצף, ההטלה השלישית מתבטלת והתור עובר לשחקן הבא."},
        {"אין מהלך חוקי", "אם אין מהלך חוקי, התור עובר לשחקן הבא, אלא אם התקבל 6 שמעניק הטלה נוספת."},
        {"אכילת כלי יריב", "נחיתה על כלי יריב יחיד במשבצת לא מוגנת מחזירה אותו לטירה."},
        {"משבצות מוגנות", "משבצות ההתחלה (#), ההגנה (★) ומסלול הבית מוגנות מאכילה. ארבע משבצות ההגנה מוגרלות בכל משחק."},
        {"החץ ומסלול הבית", "כל כלי מתקדם במסלול עד לחץ בצבע שלו, שמוביל למסלול הבית האישי."},
        {"חסימת Double Block", "שני כלים או יותר של אותו שחקן במשבצת יוצרים חסימה. יריב אינו יכול לנחות עליה או לעבור דרכה."},
        {"עצירה לפני חסימה", "אם חסימה מפריעה לתנועה, יש לבחור מהלך מלא של כלי אחר. אם אין כזה, מותר לעצור במשבצת שלפני החסימה."},
        {"כניסה לבית הסיום", "כדי להכניס כלי לבית הסיום נדרשת תוצאת קובייה מדויקת."},
        {"ניצחון וסיום המשחק", "הראשון שמכניס את ארבעת הכלים לסיום מנצח. המשחק נגמר כשנותר שחקן אחד שלא סיים, ומוצג דירוג השחקנים."}
    };

    private RulesDialog() {}

    /** Open the custom rules dialog. */
    public static void show(Component parent) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "חוקי המשחק", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setUndecorated(true);
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(Theme.koalaBackgroundGradient(getWidth(), getHeight()));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createLineBorder(FRAME, 2));
        dialog.setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(19, 23, 19, 23)));
        header.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        JLabel title = rightLabel("חוקי המשחק", 21, true, TEXT);
        JLabel subtitle = rightLabel("12 חוקים קצרים · רמה קלה", 13, false, MUTED);
        title.setAlignmentX(Component.RIGHT_ALIGNMENT);
        subtitle.setAlignmentX(Component.RIGHT_ALIGNMENT);
        headerText.add(title);
        headerText.add(Box.createVerticalStrut(3));
        headerText.add(subtitle);
        JPanel headerRight = new JPanel(new BorderLayout(12, 0));
        headerRight.setOpaque(false);
        headerRight.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        headerRight.add(new BookMark(), BorderLayout.LINE_START);
        headerRight.add(headerText, BorderLayout.CENTER);
        header.add(headerRight, BorderLayout.LINE_START);

        // Close button
        JButton close = new JButton("×");
        close.setName("rules-close");
        close.setFont(new Font(Font.DIALOG, Font.PLAIN, 25));
        close.setForeground(MUTED);
        close.setBackground(PALE);
        close.setFocusPainted(false);
        close.setContentAreaFilled(false);
        close.setBorder(new EmptyBorder(0, 8, 0, 8));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dialog.dispose());
        header.add(close, BorderLayout.LINE_END);
        root.add(header, BorderLayout.NORTH);

        // Rule cards
        JPanel cards = new JPanel();
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(16, 19, 16, 19));
        cards.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        for (int i = 0; i < RULES.length; i++) {
            JPanel card = ruleCard(i + 1, RULES[i][0], RULES[i][1]);
            card.setAlignmentX(Component.CENTER_ALIGNMENT);
            cards.add(card);
            if (i < RULES.length - 1) cards.add(Box.createVerticalStrut(9));
        }

        // Scrollable content
        JScrollPane scroll = new JScrollPane(cards, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        root.add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(14, 20, 14, 20)));
        JButton done = new FilledButton("הבנתי, בואו נשחק");
        done.setName("rules-done");
        done.addActionListener(e -> dialog.dispose());
        footer.add(done, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        // Keyboard shortcut
        dialog.getRootPane().setDefaultButton(done);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "closeRules");
        dialog.getRootPane().getActionMap().put("closeRules", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        // Dialog configuration
        dialog.setSize(650, 665);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                scroll.getViewport().setViewPosition(new Point(0, 0));
            }
        });
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /** Create a numbered rule card. */
    private static JPanel ruleCard(int number, String heading, String description) {
        RoundedCard card = new RoundedCard();
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 98));
        card.setPreferredSize(new Dimension(575, 98));

        // Number badge
        JPanel badge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SOFT_ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(31, 31));
        JLabel count = new JLabel(String.valueOf(number));
        count.setForeground(ACCENT);
        count.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        badge.add(count);
        JPanel badgeWrapper = new JPanel(new BorderLayout());
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(badge, BorderLayout.NORTH);
        card.add(badgeWrapper, BorderLayout.LINE_START);

        // Rule text
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = rightLabel(heading, 15, true, TEXT);
        text.add(title, BorderLayout.NORTH);
        JTextPane explanation = new JTextPane();
        explanation.setText(description);
        explanation.setEditable(false);
        explanation.setFocusable(false);
        explanation.setOpaque(false);
        explanation.setBorder(null);
        explanation.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        explanation.setForeground(MUTED);
        explanation.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        StyledDocument doc = explanation.getStyledDocument();
        SimpleAttributeSet paragraph = new SimpleAttributeSet();
        StyleConstants.setAlignment(paragraph, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setLineSpacing(paragraph, 0.22f);
        doc.setParagraphAttributes(0, doc.getLength(), paragraph, false);
        text.add(explanation, BorderLayout.CENTER);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    /** Create a right-aligned label. */
    private static JLabel rightLabel(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(new Font(Font.DIALOG, bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        label.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return label;
    }

    /** Draw the book icon. */
    private static final class BookMark extends JComponent {
        BookMark() {
            setPreferredSize(new Dimension(45, 45));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(1, 1, 43, 43, 13, 13);
            g2.setColor(BORDER);
            g2.drawRoundRect(1, 1, 43, 43, 13, 13);
            g2.setColor(ACCENT);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(12, 12, 21, 22, 3, 3);
            g2.drawLine(17, 18, 28, 18);
            g2.drawLine(17, 23, 28, 23);
            g2.drawLine(17, 28, 25, 28);
            g2.dispose();
        }
    }

    /** Draw a rounded white card. */
    private static final class RoundedCard extends JPanel {
        RoundedCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_SURFACE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(new Color(102, 170, 132));
            g2.fillRoundRect(getWidth() - 6, 13, 3, getHeight() - 26, 3, 3);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Styled primary button. */
    private static final class FilledButton extends JButton {
        FilledButton(String caption) {
            super(caption);
            setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(580, 45));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = getModel().isPressed() ? Theme.KOALA_PRIMARY_DARK : getModel().isRollover() ? ACCENT_HOVER : ACCENT;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
