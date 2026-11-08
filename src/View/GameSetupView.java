package View;

import Model.Difficulty;
import Model.GameRules;
import Model.PlayerColor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * View responsible for configuring a new game.
 * Collects player names, colors and player count.
 * Sends the selected settings to the Controller through StartListener.
 * Input validation and game rules remain in the Model.
 */
public final class GameSetupView extends JPanel {

    private static final long serialVersionUID = 1L;

    @FunctionalInterface
    public interface StartListener {
        void onStart(Difficulty difficulty, List<String> names, List<PlayerColor> colors);
    }

    private static final Color GREEN = Theme.KOALA_PRIMARY;
    private static final Color GREEN_DARK = Theme.KOALA_PRIMARY_DARK;
    private static final Color TEXT = Theme.KOALA_TEXT;
    private static final Color MUTED = Theme.KOALA_MUTED;
    private static final Color ROW_BACKGROUND = Theme.KOALA_ROW_BACKGROUND;
    private static final Color BORDER = Theme.KOALA_BORDER;
    private static final Color ERROR = Theme.KOALA_ERROR;

    private final JTextField[] names = new JTextField[GameRules.MAX_PLAYERS];
    private final PlayerColor[] selectedColors = new PlayerColor[GameRules.MAX_PLAYERS];
    private final JPanel[] playerRows = new JPanel[GameRules.MAX_PLAYERS];
    private final ColorButton[][] colorButtons = new ColorButton[GameRules.MAX_PLAYERS][PlayerColor.values().length];
    private final JToggleButton[] playerCountButtons = new JToggleButton[GameRules.MAX_PLAYERS - GameRules.MIN_PLAYERS + 1];
    private final JLabel errorLabel = new JLabel();

    private int playerCount = GameRules.MIN_PLAYERS;

    // Initializes the game setup screen and connects navigation callbacks
	public GameSetupView(StartListener onStart, Runnable onBack) {
	    Objects.requireNonNull(onStart);
	    Objects.requireNonNull(onBack);
	
	    setLayout(new BorderLayout());
	    setOpaque(false);
	    setBorder(new EmptyBorder(38, 24, 18, 24));
	
	    JPanel content = new JPanel() {
	        private static final long serialVersionUID = 1L;
	
	        @Override
	        public Dimension getPreferredSize() {
	            Dimension preferred = super.getPreferredSize();
	            return new Dimension(890, preferred.height);
	        }
	    };
	
	    content.setOpaque(false);
	    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
	
	    content.add(createHeader(onBack));
	    content.add(Box.createVerticalStrut(14));
	    content.add(createSettingsSection());
	    content.add(Box.createVerticalStrut(14));
	    content.add(createPlayersSection());
	
	    FullWidthScrollPanel centered = new FullWidthScrollPanel();
	    centered.setOpaque(false);
	
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.weightx = 1.0;
	    gbc.weighty = 1.0;
	    gbc.anchor = GridBagConstraints.NORTH;
	    gbc.fill = GridBagConstraints.NONE;
	    centered.add(content, gbc);
	
	    JScrollPane scrollPane = new JScrollPane(centered, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	
	    scrollPane.setBorder(null);
	    scrollPane.setOpaque(false);
	    scrollPane.getViewport().setOpaque(false);
	    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
	
	    add(scrollPane, BorderLayout.CENTER);
	
	    // The footer remains outside the scrollable player configuration.
	    JPanel footerContainer = new JPanel(new GridBagLayout());
	    footerContainer.setOpaque(false);
	    footerContainer.setBorder(new EmptyBorder(16, 0, 34, 0));
	
	    JPanel footer = createFooter(onStart);
	    footer.setPreferredSize(new Dimension(890, 54));
	
	    GridBagConstraints footerConstraints = new GridBagConstraints();
	    footerConstraints.gridx = 0;
	    footerConstraints.gridy = 0;
	    footerConstraints.anchor = GridBagConstraints.CENTER;
	
	    footerContainer.add(footer, footerConstraints);
	    add(footerContainer, BorderLayout.SOUTH);
	
	    updatePlayerRows();
	}

	private JPanel createHeader(Runnable onBack) {
	    JPanel header = new JPanel(new BorderLayout(12, 0));
	    header.setOpaque(false);
	
	    JLabel title = label("הגדרת משחק חדש", 28, true, TEXT);
	    JLabel subtitle = label("בחרו שחקנים, רמת קושי וצבעים", 14, false, MUTED);
	
	    JPanel heading = new JPanel();
	    heading.setOpaque(false);
	    heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
	
	    title.setAlignmentX(Component.RIGHT_ALIGNMENT);
	    subtitle.setAlignmentX(Component.RIGHT_ALIGNMENT);
	
	    heading.add(title);
	    heading.add(Box.createVerticalStrut(4));
	    heading.add(subtitle);
	
	    header.add(heading, BorderLayout.LINE_START);
	
	    JButton backButton = createBackButton();
	    backButton.addActionListener(e -> onBack.run());
	
	    JPanel backWrapper = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
	    backWrapper.setOpaque(false);
	    backWrapper.add(backButton);
	
	    header.add(backWrapper, BorderLayout.LINE_END);
	    return header;
	}
	

	private static JButton createBackButton() {
	    JButton button = new JButton("חזרה") {
	        private static final long serialVersionUID = 1L;
	
	        @Override
	        protected void paintComponent(Graphics graphics) {
	            Graphics2D g = (Graphics2D) graphics.create();
	            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	
	            Color background = Color.WHITE;
	
	            if (getModel().isPressed()) {
	                background = new Color(212, 231, 219);
	            } else if (getModel().isRollover()) {
	                background = new Color(232, 245, 237);
	            }
	
	            g.setColor(background);
	            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
	
	            g.setColor(BORDER);
	            g.setStroke(new BasicStroke(1.2f));
	            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
	
	            g.dispose();
	            super.paintComponent(graphics);
	        }
	    };
	
	    button.setPreferredSize(new Dimension(110, 44));
	    button.setFont(Theme.font(15, true));
	    button.setForeground(GREEN_DARK);
	    button.setFocusPainted(false);
	    button.setContentAreaFilled(false);
	    button.setBorderPainted(false);
	    button.setOpaque(false);
	    button.setRolloverEnabled(true);
	    button.setBorder(new EmptyBorder(10, 18, 10, 18));
	    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	
	    return button;
	}


    private JPanel createSettingsSection() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 15), Color.WHITE, BORDER, 18);
        card.add(sectionHeading(1, "מספר שחקנים ורמת קושי"), BorderLayout.NORTH);

        JPanel options = new JPanel(new GridLayout(1, 2, 20, 0));
        options.setOpaque(false);
        options.add(createPlayerCountPanel());
        options.add(createDifficultyPanel());

        card.add(options, BorderLayout.CENTER);
        refreshPlayerCountButtons();

        return card;
    }

    private JPanel createPlayerCountPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 9));
        panel.setOpaque(false);
        panel.add(label("מספר שחקנים", 13, true, MUTED), BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(1, playerCountButtons.length, 9, 0));
        choices.setOpaque(false);

        ButtonGroup group = new ButtonGroup();

        for (int count = GameRules.MIN_PLAYERS; count <= GameRules.MAX_PLAYERS; count++) {
            final int selectedCount = count;

            JToggleButton button = new JToggleButton(String.valueOf(count));
            button.setName("player-count-" + count);
            button.setFont(Theme.font(16, true));
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setPreferredSize(new Dimension(74, 42));
            button.addActionListener(e -> setPlayerCount(selectedCount));

            playerCountButtons[count - GameRules.MIN_PLAYERS] = button;
            group.add(button);
            choices.add(button);
        }

        panel.add(choices, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDifficultyPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 9));
        panel.setOpaque(false);
        panel.add(label("רמת קושי", 13, true, MUTED), BorderLayout.NORTH);

        JPanel levels = new JPanel(new GridLayout(1, 3, 8, 0));
        levels.setOpaque(false);

        levels.add(createDifficultyCard("קלה", "זמינה", true));
        levels.add(createDifficultyCard("בינונית", "בקרוב", false));
        levels.add(createDifficultyCard("קשה", "בקרוב", false));

        panel.add(levels, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDifficultyCard(String title, String description, boolean available) {
        Color background = available ? new Color(232, 247, 239) : ROW_BACKGROUND;
        Color outline = available ? GREEN : BORDER;

        RoundedPanel card = new RoundedPanel(new GridLayout(2, 1, 0, 2), background, outline, 10);
        card.setName("difficulty-" + title);

        JLabel heading = label((available ? "✓ " : "נעול · ") + title, 14, true, available ? GREEN_DARK : MUTED);
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel hint = label(description, 11, false, MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(heading);
        card.add(hint);
        card.setToolTipText(available ? "רמה קלה זמינה באיטרציה הנוכחית" : "הרמה תהיה זמינה באיטרציה עתידית");

        return card;
    }

    private JPanel createPlayersSection() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 13), Color.WHITE, BORDER, 18);
        card.add(sectionHeading(2, "שם וצבע לכל שחקן"), BorderLayout.NORTH);

        JPanel playerList = new JPanel();
        playerList.setOpaque(false);
        playerList.setLayout(new BoxLayout(playerList, BoxLayout.Y_AXIS));

        for (int i = 0; i < GameRules.MAX_PLAYERS; i++) {
            playerRows[i] = createPlayerRow(i);
            playerList.add(playerRows[i]);
        }

        card.add(playerList, BorderLayout.CENTER);

        JPanel notices = new JPanel();
        notices.setOpaque(false);
        notices.setLayout(new BoxLayout(notices, BoxLayout.Y_AXIS));

        JLabel hint = label("צבע שנבחר נשאר מוצג, אך אינו זמין לשחקנים האחרים.", 12, false, MUTED);
        hint.setAlignmentX(Component.RIGHT_ALIGNMENT);
        notices.add(hint);

        errorLabel.setName("setup-error");
        errorLabel.setForeground(ERROR);
        errorLabel.setFont(Theme.font(13, true));
        errorLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        errorLabel.setVisible(false);

        notices.add(errorLabel);
        card.add(notices, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createPlayerRow(int index) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setName("player-row-" + index);
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 9, 0));

        RoundedPanel row = new RoundedPanel(new BorderLayout(13, 0), ROW_BACKGROUND, BORDER, 10);

        JLabel playerLabel = label("שחקן " + (index + 1), 14, true, TEXT);
        playerLabel.setPreferredSize(new Dimension(72, 38));
        playerLabel.setVerticalAlignment(SwingConstants.CENTER);
        row.add(playerLabel, BorderLayout.LINE_START);

        JTextField nameField = Theme.textField();
        nameField.setName("player-name-" + index);
        nameField.setPreferredSize(new Dimension(250, 38));
        nameField.setToolTipText("שם או כינוי, עד " + GameRules.MAX_NAME_LENGTH + " תווים");

        ((AbstractDocument) nameField.getDocument()).setDocumentFilter(new NameLengthFilter());

        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearError();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearError();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clearError();
            }
        });

        names[index] = nameField;
        row.add(nameField, BorderLayout.CENTER);

        JPanel palette = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        palette.setOpaque(false);

        for (PlayerColor color : PlayerColor.values()) {
            ColorButton button = new ColorButton(color);
            button.setName("player-color-" + index + "-" + color.name());
            button.getAccessibleContext().setAccessibleName("בחירת צבע " + color.displayName() + " לשחקן " + (index + 1));

            button.addActionListener(e -> chooseColor(index, color));
            colorButtons[index][color.ordinal()] = button;
            palette.add(button);
        }

        row.add(palette, BorderLayout.LINE_END);
        wrapper.add(row);

        return wrapper;
    }


	private JPanel createFooter(StartListener onStart) {
	    JPanel footer = new JPanel(new BorderLayout());
	    footer.setOpaque(false);
	
	    JButton startButton = actionButton("התחלת משחק", true);
	    startButton.setPreferredSize(new Dimension(185, 50));
	    startButton.setMinimumSize(new Dimension(185, 50));
	    startButton.setMaximumSize(new Dimension(185, 50));
	
	    startButton.addActionListener(e -> onStart.onStart(Difficulty.EASY, getSelectedNames(), getSelectedColors()));
	
	    JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
	    buttonWrapper.setOpaque(false);
	    buttonWrapper.add(startButton);
	
	    footer.add(buttonWrapper, BorderLayout.LINE_START);
	
	    JLabel hint = label("יש לבחור שם וצבע ייחודיים לכל שחקן", 12, false, MUTED);
	    hint.setVerticalAlignment(SwingConstants.CENTER);
	    footer.add(hint, BorderLayout.LINE_END);
	
	    return footer;
	}

	/**
	 * Updates the number of active players.
	 * Releases colors assigned to players who become inactive
	 * and refreshes the visible player rows.
	 */
	private void setPlayerCount(int count) {
	    if (count < GameRules.MIN_PLAYERS || count > GameRules.MAX_PLAYERS) {
	        return;
	    }
	
	    playerCount = count;
	
	    // Release colors belonging to players who are no longer active.
	    for (int i = count; i < selectedColors.length; i++) {
	        selectedColors[i] = null;
	    }
	
	    clearError();
	    updatePlayerRows();
	}

    private void updatePlayerRows() {
        for (int i = 0; i < playerRows.length; i++) {
            playerRows[i].setVisible(i < playerCount);
        }

        refreshPlayerCountButtons();
        refreshColorButtons();

        revalidate();
        repaint();
    }

    private void refreshPlayerCountButtons() {
        for (int i = 0; i < playerCountButtons.length; i++) {
            JToggleButton button = playerCountButtons[i];
            boolean selected = playerCount == i + GameRules.MIN_PLAYERS;

            button.setSelected(selected);
            button.setBackground(selected ? GREEN : Color.WHITE);
            button.setForeground(selected ? Color.WHITE : TEXT);
            button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selected ? GREEN : BORDER), new EmptyBorder(7, 15, 7, 15)));
        }
    }

    /**
     * Handles color selection for a player.
     * Clicking an already selected color deselects it.
     * Colors assigned to other active players cannot be selected.
     */
    private void chooseColor(int playerIndex, PlayerColor color) {
    	if (playerIndex >= playerCount) {
    		return;
    	}

	    // Clicking the selected color again removes the selection.
	    if (selectedColors[playerIndex] == color) {
	        selectedColors[playerIndex] = null;
	    } else {
	        // Prevent duplicate colors among active players.
	        for (int i = 0; i < playerCount; i++) {
	            if (i != playerIndex && selectedColors[i] == color) {
	                return;
	            }
	        }
	
	        selectedColors[playerIndex] = color;
	    }
	
	    clearError();
	    refreshColorButtons();
	}

    /**
     * Synchronizes color buttons with the current selections.
     * Selected colors are marked, while colors occupied by
     * other players remain visible but disabled.
     */
    private void refreshColorButtons() {
        for (int player = 0; player < colorButtons.length; player++) {
            for (PlayerColor color : PlayerColor.values()) {
                ColorButton button = colorButtons[player][color.ordinal()];
                boolean selected = selectedColors[player] == color;
                boolean taken = false;

                for (int other = 0; other < playerCount; other++) {
                    if (other != player && selectedColors[other] == color) {
                        taken = true;
                        break;
                    }
                }

                button.setSelected(selected);
                button.setEnabled(!taken);
                button.setToolTipText(taken ? "הצבע תפוס על ידי שחקן אחר" : color.displayName());
                button.repaint();
            }
        }
    }

    private List<String> getSelectedNames() {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            result.add(names[i].getText());
        }

        return result;
    }

    private List<PlayerColor> getSelectedColors() {
        List<PlayerColor> result = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            result.add(selectedColors[i]);
        }

        return result;
    }

    /**
     * Displays a validation error without leaving the setup screen.
     */
    public void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        revalidate();
        repaint();
    }

    private void clearError() {
        if (errorLabel.isVisible()) {
            errorLabel.setVisible(false);
            errorLabel.setText("");
        }
    }

    private static JLabel label(String text, int size, boolean bold, Color foreground) {
        JLabel result = Theme.label(text, size, bold);
        result.setForeground(foreground);
        return result;
    }

    private static JPanel sectionHeading(int number, String title) {
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        heading.setOpaque(false);

        JLabel step = label(" " + number + " ", 13, true, Color.WHITE);
        step.setOpaque(true);
        step.setBackground(GREEN);
        step.setBorder(new EmptyBorder(3, 8, 3, 8));

        heading.add(step);
        heading.add(label(title, 16, true, TEXT));

        return heading;
    }

    private static JButton actionButton(String text, boolean primary) {
        JButton button = new JButton(text);

        button.setFont(Theme.font(15, true));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(primary ? GREEN : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary ? GREEN : BORDER), new EmptyBorder(11, 24, 11, 24)));

        return button;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        g.setPaint(Theme.koalaBackgroundGradient(getWidth(), getHeight()));

        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();

        super.paintComponent(graphics);
    }

    /**
     * Panel with rounded corners and a thin border.
     */
    private static final class RoundedPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private final Color fill;
        private final Color outline;

        RoundedPanel(LayoutManager layout, Color fill, Color outline, int inset) {
            super(layout);

            this.fill = fill;
            this.outline = outline;

            setOpaque(false);
            setBorder(new EmptyBorder(inset, inset + 2, inset, inset + 2));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);

            g.setColor(outline);
            g.setStroke(new BasicStroke(1.3f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);

            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /**
     * Color selection button.
     * Colors assigned to other players stay visible but appear dimmed.
     */
    private static final class ColorButton extends JToggleButton {

        private static final long serialVersionUID = 1L;

        private final PlayerColor color;

        ColorButton(PlayerColor color) {
            this.color = color;

            setPreferredSize(new Dimension(39, 39));
            setMinimumSize(new Dimension(39, 39));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g.setComposite(AlphaComposite.SrcOver.derive(0.24f));
            }

            int x = (getWidth() - 27) / 2;
            int y = (getHeight() - 27) / 2;

            g.setColor(Theme.colorOf(color));
            g.fillOval(x, y, 27, 27);

            if (isSelected()) {
                g.setColor(TEXT);
                g.setStroke(new BasicStroke(2.0f));
                g.drawOval(x - 4, y - 4, 35, 35);

                g.setColor(color == PlayerColor.YELLOW ? TEXT : Color.WHITE);
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Draw a check mark on the selected color.
                g.drawLine(x + 7, y + 14, x + 12, y + 19);
                g.drawLine(x + 12, y + 19, x + 21, y + 9);
            }

            g.dispose();
        }
    }

    /**
     * Restricts player names to the maximum length defined in GameRules.
     */
    private static final class NameLengthFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, string, attrs);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {

            if (text == null) {
                super.replace(fb, offset, length, null, attrs);
                return;
            }

            int remaining = GameRules.MAX_NAME_LENGTH - (fb.getDocument().getLength() - length);
            remaining = Math.max(0, remaining);

            if (text.length() > remaining) {
                text = text.substring(0, remaining);
            }

            super.replace(fb, offset, length, text, attrs);
        }
    }
    
    /**
     * Scrollable container that tracks the viewport width.
     * Keeps the setup form centered and allows vertical scrolling
     * when additional player rows require more space.
     */
	private static final class FullWidthScrollPanel extends JPanel implements Scrollable {
	    private static final long serialVersionUID = 1L;
	
	    FullWidthScrollPanel() {
	        super(new GridBagLayout());
	    }
	
	    @Override
	    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
	
	    @Override
	    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
	
	    @Override
	    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
	        return Math.max(16, visibleRect.height - 32);
	    }
	
	    @Override
	    public boolean getScrollableTracksViewportWidth() { return true; }
	
	    @Override
	    public boolean getScrollableTracksViewportHeight() { return false; }
	}
}