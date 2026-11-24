package View;

import Model.Question;
import Model.QuestionLevel;
import Model.QuestionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

/** A responsive, scroll-free Hebrew question dialog and short answer feedback for Ludo Koala. */
public final class QuestionDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Color PURPLE = new Color(105, 70, 180);
    private static final Color DEEP_PURPLE = new Color(78, 53, 141);
    private static final Color INK = new Color(45, 36, 70);
    private static final Color MUTED = new Color(111, 100, 133);
    private static final Color BACKGROUND = new Color(248, 246, 253);
    private static final Color GREEN = new Color(22, 154, 99);
    private static final Color RED = new Color(210, 68, 86);
    private static final char[] LETTERS = {'א', 'ב', 'ג', 'ד'};

    private final AnswerCard[] answers = new AnswerCard[4];
    private final JButton submit = new ConfirmButton("אישור תשובה  ←");
    private int selectedIndex = -1;

    /** Build a question dialog that adapts to question length without scroll bars. */
    private QuestionDialog(Window owner, Question question) {
        super(owner, "שאלת ידע · Ludo Koala", Dialog.ModalityType.APPLICATION_MODAL);
        Objects.requireNonNull(question, "question");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = Math.min(940, Math.max(480, screen.width - 48));
        int maxHeight = Math.max(440, screen.height - 40);

        // Prefer comfortable text, but tighten all four answer cards for long questions.
        for (int font = 19; font >= 11; font--) {
            JPanel content = buildContent(question, width, font, Math.max(11, font - 3));
            setContentPane(content);
            pack();
            if (getHeight() <= maxHeight || font == 11) break;
        }
        // The model answers are always shown at once, never inside a JScrollPane.
        setResizable(false);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(submit);
    }

    /** Display a modal question on the Swing event thread and return the selected index. */
    public static int showQuestion(Component parent, Question question) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Question dialog must be shown on the Swing EDT");
        }
        Window owner = parent instanceof Window window ? window
                : SwingUtilities.getWindowAncestor(parent);
        QuestionDialog dialog = new QuestionDialog(owner, question);
        dialog.setVisible(true);
        return dialog.selectedIndex;
    }

    /** Assemble a top-to-bottom RTL dialog with room for every answer and no scroll pane. */
    private JPanel buildContent(Question question, int width, int questionFont, int answerFont) {
        boolean compact = questionFont <= 16;
        int headerHeight = compact ? 76 : 92;
        int topPad = compact ? 7 : 15;
        int bottomPad = compact ? 7 : 12;
        int cardPad = compact ? 8 : 13;
        int gapAfterQuestion = compact ? 7 : 13;
        int itemGap = compact ? 5 : 8;
        int innerWidth = width - 56;
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.setPreferredSize(new Dimension(width, 1));

        GradientHeader header = new GradientHeader();
        header.setPreferredSize(new Dimension(width, headerHeight));
        header.setBorder(new EmptyBorder(compact ? 5 : 11, 28, compact ? 5 : 11, 28));
        header.setLayout(new BorderLayout(16, 0));
        header.add(new JLabel(new KoalaIcon(compact ? 54 : 65)), BorderLayout.WEST);
        JPanel headings = new JPanel();
        headings.setOpaque(false);
        headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
        JLabel title = textLabel("שאלת הידע שלך", compact ? 21 : 25, Font.BOLD, Color.WHITE);
        JLabel subtitle = textLabel("רמה " + levelName(question.level())
                + "    •    נכון: +" + steps(question.level())
                + " צעדים    •    שגוי: −" + steps(question.level()) + " צעדים",
                compact ? 11 : 13, Font.BOLD, new Color(246, 239, 255));
        title.setAlignmentX(Component.RIGHT_ALIGNMENT);
        subtitle.setAlignmentX(Component.RIGHT_ALIGNMENT);
        headings.add(Box.createVerticalGlue());
        headings.add(title);
        headings.add(Box.createVerticalStrut(compact ? 3 : 7));
        headings.add(subtitle);
        headings.add(Box.createVerticalGlue());
        header.add(headings, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setBackground(BACKGROUND);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(topPad, 28, bottomPad, 28));

        if (!compact) {
            JLabel promptLabel = textLabel("השאלה", 13, Font.BOLD, PURPLE);
            promptLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            body.add(promptLabel);
            body.add(Box.createVerticalStrut(6));
        }

        String displayedQuestion = normalize(question.text());
        if (compact && displayedQuestion.length() > 290) displayedQuestion = displayedQuestion.replace("\n\n", "\n");
        JTextArea questionText = makeWrappedText(displayedQuestion, questionFont, Font.BOLD, INK);
        int questionTextWidth = innerWidth - 2 * cardPad - 2;
        int questionTextHeight = measureText(questionText, questionTextWidth);
        JPanel questionCard = new RoundedSurface(Color.WHITE, new Color(229, 221, 243));
        questionCard.setLayout(new BorderLayout());
        questionCard.setBorder(new EmptyBorder(cardPad, cardPad, cardPad, cardPad));
        questionCard.add(questionText, BorderLayout.CENTER);
        questionCard.setPreferredSize(new Dimension(innerWidth, questionTextHeight + cardPad * 2));
        questionCard.setMaximumSize(questionCard.getPreferredSize());
        questionCard.setAlignmentX(Component.RIGHT_ALIGNMENT);
        body.add(questionCard);
        body.add(Box.createVerticalStrut(gapAfterQuestion));

        if (!compact) {
            JLabel choicesLabel = textLabel("בחרי תשובה אחת", 13, Font.BOLD, MUTED);
            choicesLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            body.add(choicesLabel);
            body.add(Box.createVerticalStrut(7));
        }
        for (int i = 0; i < 4; i++) {
            final int index = i;
            AnswerCard card = new AnswerCard(LETTERS[i], question.options().get(i),
                    innerWidth, answerFont, compact, () -> select(index));
            card.setAlignmentX(Component.RIGHT_ALIGNMENT);
            answers[i] = card;
            body.add(card);
            if (i != 3) body.add(Box.createVerticalStrut(itemGap));
        }
        root.add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(14, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(compact ? 6 : 10, 28, compact ? 6 : 10, 28));
        JLabel hint = textLabel("בחרי תשובה ולאחר מכן לחצי על אישור", 13, Font.PLAIN, MUTED);
        footer.add(hint, BorderLayout.EAST);
        submit.setPreferredSize(new Dimension(178, compact ? 37 : 45));
        submit.setEnabled(false);
        for (var listener : submit.getActionListeners()) submit.removeActionListener(listener);
        submit.addActionListener(e -> { if (selectedIndex >= 0) dispose(); });
        footer.add(submit, BorderLayout.WEST);
        root.add(footer, BorderLayout.SOUTH);

        int totalHeight = headerHeight + body.getPreferredSize().height + footer.getPreferredSize().height;
        root.setPreferredSize(new Dimension(width, totalHeight));
        return root;
    }

    /** Keep an explicit RTL text component so Hebrew and English words wrap naturally. */
    private static JTextArea makeWrappedText(String raw, int size, int style, Color color) {
        JTextArea field = new JTextArea(normalize(raw));
        field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        field.setEditable(false);
        field.setFocusable(false);
        field.setOpaque(false);
        field.setFont(new Font("Dialog", style, size));
        field.setForeground(color);
        field.setMargin(new Insets(0, 0, 0, 0));
        return field;
    }

    /** Measure the actual wrapped text height before laying out the window. */
    private static int measureText(JTextArea text, int width) {
        text.setSize(new Dimension(width, 10_000));
        return Math.max(text.getFontMetrics(text.getFont()).getHeight(), text.getPreferredSize().height);
    }

    /** Display the backslash-n sequences from the JSON file as real line breaks. */
    private static String normalize(String raw) {
        String plain = Objects.requireNonNull(raw).replace("\\r\\n", "\n").replace("\\n", "\n")
                .replace("\\t", "    ").replace("\r\n", "\n");
        StringBuilder result = new StringBuilder();
        String[] lines = plain.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.stripLeading();
            boolean isCode = trimmed.startsWith("public ") || trimmed.startsWith("if (")
                    || trimmed.startsWith("return ") || trimmed.equals("}")
                    || trimmed.equals("{") || trimmed.startsWith("//");
            if (isCode) result.append('\u202A').append(line).append('\u202C');
            else result.append(line);
            if (i + 1 < lines.length) result.append('\n');
        }
        return result.toString();
    }

    /** Create a correctly aligned Hebrew label with a predictable font. */
    private static JLabel textLabel(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        label.setFont(new Font("Dialog", style, size));
        label.setForeground(color);
        return label;
    }

    /** Mark exactly one choice and enable confirmation. */
    private void select(int index) {
        selectedIndex = index;
        for (int i = 0; i < answers.length; i++) answers[i].setSelectedCard(i == index);
        submit.setEnabled(true);
    }

    /** Show a compact, non-blocking green or red result toast after Model validation. */
    public static void showAnswerFeedback(Component parent, QuestionService.AnswerResult result) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showAnswerFeedback(parent, result));
            return;
        }
        Objects.requireNonNull(result, "result");
        Window owner = parent instanceof Window window ? window
                : SwingUtilities.getWindowAncestor(parent);
        JWindow toast = owner == null ? new JWindow() : new JWindow(owner);
        FeedbackPanel panel = new FeedbackPanel(result.correct(), steps(result.level()));
        toast.setBackground(new Color(0, 0, 0, 0));
        toast.setContentPane(panel);
        toast.setSize(430, 132);
        toast.setLocationRelativeTo(owner);
        toast.setVisible(true);
        final int[] tick = {0};
        Timer timer = new Timer(40, null);
        timer.addActionListener(e -> {
            panel.setTick(++tick[0]);
            if (tick[0] >= 28) {
                timer.stop();
                toast.dispose();
            }
        });
        timer.start();
    }

    /** Return the movement size for each question difficulty. */
    private static int steps(QuestionLevel level) {
        return switch (level) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
    }

    /** Localize each question difficulty. */
    private static String levelName(QuestionLevel level) {
        return switch (level) {
            case EASY -> "קלה";
            case MEDIUM -> "בינונית";
            case HARD -> "קשה";
        };
    }

    /** Paint the rounded background of the question. */
    private static class RoundedSurface extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        private final Color line;
        private RoundedSurface(Color fill, Color line) {
            this.fill = fill;
            this.line = line;
            setOpaque(false);
        }
        /** Draw the surface border without modifying text layout. */
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            g.setColor(line);
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** A compact selectable answer card whose answer and letter are RTL. */
    private static final class AnswerCard extends RoundedSurface {
        private static final long serialVersionUID = 1L;
        private boolean selectedCard;
        private boolean hover;
        private final JLabel badge;

        /** Create one wide, word-wrapped answer with its Hebrew letter on the right. */
        private AnswerCard(char letter, String answer, int width, int font, boolean compact, Runnable action) {
            super(Color.WHITE, new Color(222, 215, 237));
            int inset = compact ? 8 : 11;
            setLayout(new BorderLayout(compact ? 8 : 13, 0));
            setBorder(new EmptyBorder(inset, 17, inset, 17));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            badge = new JLabel(String.valueOf(letter), SwingConstants.CENTER);
            badge.setOpaque(true);
            badge.setBackground(new Color(238, 230, 252));
            badge.setForeground(DEEP_PURPLE);
            badge.setFont(new Font("Dialog", Font.BOLD, 16));
            badge.setPreferredSize(new Dimension(compact ? 28 : 35, compact ? 28 : 35));
            badge.setBorder(BorderFactory.createLineBorder(new Color(211, 195, 239), 1));
            add(badge, BorderLayout.EAST);

            JTextArea answerText = makeWrappedText(answer, font, Font.PLAIN, INK);
            int wrapWidth = width - 17 - 17 - (compact ? 28 : 35) - (compact ? 8 : 13) - 4;
            int height = measureText(answerText, wrapWidth);
            add(answerText, BorderLayout.CENTER);
            setPreferredSize(new Dimension(width, Math.max(compact ? 44 : 57, height + inset * 2)));
            setMaximumSize(getPreferredSize());

            MouseAdapter mouse = new MouseAdapter() {
                /** Select a choice when clicking the card, answer text or its letter. */
                @Override public void mouseClicked(MouseEvent e) { action.run(); requestFocusInWindow(); }
                /** Highlight the card without changing the selected answer. */
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                /** Clear hover color. */
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            };
            addMouseListener(mouse);
            answerText.addMouseListener(mouse);
            badge.addMouseListener(mouse);
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "choose");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "choose");
            getActionMap().put("choose", new AbstractAction() {
                private static final long serialVersionUID = 1L;
                /** Activate this answer from the keyboard. */
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
            });
        }

        /** Show which answer is selected. */
        private void setSelectedCard(boolean selected) {
            selectedCard = selected;
            badge.setBackground(selected ? PURPLE : new Color(238, 230, 252));
            badge.setForeground(selected ? Color.WHITE : DEEP_PURPLE);
            repaint();
        }

        /** Render a lavender selected/hover state without relying on HTML labels. */
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(selectedCard ? new Color(237, 229, 252)
                    : hover ? new Color(250, 246, 255) : Color.WHITE);
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            g.setColor(selectedCard ? PURPLE : hover ? new Color(170, 143, 216)
                    : new Color(224, 218, 238));
            g.setStroke(new BasicStroke(selectedCard ? 2.3f : 1.2f));
            g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 22, 22);
            g.dispose();
        }
    }

    /** A polished purple confirmation button. */
    private static final class ConfirmButton extends JButton {
        private static final long serialVersionUID = 1L;
        /** Initialize a Hebrew confirmation button. */
        private ConfirmButton(String text) {
            super(text);
            setFont(new Font("Dialog", Font.BOLD, 15));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        /** Paint an accessible disabled state and a solid selection color. */
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isEnabled() ? PURPLE : new Color(202, 193, 220));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Paint the title strip and subtle playful decorations. */
    private static final class GradientHeader extends JPanel {
        private static final long serialVersionUID = 1L;
        /** Fill a calm purple gradient, keeping high-contrast text. */
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setPaint(new GradientPaint(0, 0, new Color(146, 98, 215),
                    getWidth(), getHeight(), DEEP_PURPLE));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 255, 255, 23));
            g.fillOval(getWidth() - 100, -70, 155, 155);
            g.dispose();
        }
    }

    /** Draw a tiny koala avatar directly in Java, with no additional image files. */
    private static final class KoalaIcon implements Icon {
        private final int size;
        private KoalaIcon(int size) { this.size = size; }
        /** Return the icon width. */
        @Override public int getIconWidth() { return size; }
        /** Return the icon height. */
        @Override public int getIconHeight() { return size; }
        /** Paint koala ears, face, eyes and nose. */
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            double scale = size / 80.0;
            g.translate(x, y);
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(214, 205, 232));
            g.fillOval(1, 9, 31, 34);
            g.fillOval(48, 9, 31, 34);
            g.setColor(new Color(245, 170, 200));
            g.fillOval(9, 15, 19, 22);
            g.fillOval(53, 15, 19, 22);
            g.setColor(new Color(232, 229, 243));
            g.fillOval(13, 11, 55, 65);
            g.setColor(INK);
            g.fillOval(28, 40, 7, 8);
            g.fillOval(49, 40, 7, 8);
            g.fillOval(34, 48, 17, 19);
            g.dispose();
        }
    }

    /** Display a green or red feedback card with a brief pulsing outline. */
    private static final class FeedbackPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final boolean correct;
        private int tick;
        /** Create the answer-feedback panel. */
        private FeedbackPanel(boolean correct, int steps) {
            this.correct = correct;
            setOpaque(false);
            setLayout(new BorderLayout(16, 0));
            setBorder(new EmptyBorder(26, 32, 26, 32));
            JLabel symbol = new JLabel(correct ? "✓" : "✕", SwingConstants.CENTER);
            symbol.setFont(new Font("Dialog", Font.BOLD, 32));
            symbol.setForeground(Color.WHITE);
            symbol.setOpaque(true);
            symbol.setBackground(correct ? GREEN : RED);
            symbol.setPreferredSize(new Dimension(62, 62));
            add(symbol, BorderLayout.WEST);
            JPanel words = new JPanel();
            words.setOpaque(false);
            words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
            JLabel title = textLabel(correct ? "תשובה נכונה!" : "תשובה שגויה", 24, Font.BOLD, INK);
            JLabel explanation = textLabel(correct ? "מתקדמים " + steps + " צעדים"
                    : "חוזרים " + steps + " צעדים", 17, Font.BOLD, correct ? GREEN : RED);
            title.setAlignmentX(Component.RIGHT_ALIGNMENT);
            explanation.setAlignmentX(Component.RIGHT_ALIGNMENT);
            words.add(Box.createVerticalStrut(5));
            words.add(title);
            words.add(Box.createVerticalStrut(6));
            words.add(explanation);
            add(words, BorderLayout.EAST);
        }
        /** Advance the outline animation. */
        private void setTick(int value) { tick = value; repaint(); }
        /** Paint a short green/red pulse behind the feedback labels. */
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(38, 30, 53, 45));
            g.fillRoundRect(7, 10, getWidth() - 12, getHeight() - 16, 28, 28);
            g.setColor(Color.WHITE);
            g.fillRoundRect(4, 4, getWidth() - 12, getHeight() - 16, 25, 25);
            Color accent = correct ? GREEN : RED;
            float pulse = (float) (0.5 + 0.5 * Math.sin(tick * 0.55));
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    155 + Math.round(90 * pulse)));
            g.setStroke(new BasicStroke(2.5f + pulse * 1.5f));
            g.drawRoundRect(6, 6, getWidth() - 16, getHeight() - 20, 25, 25);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
