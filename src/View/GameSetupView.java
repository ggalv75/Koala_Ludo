package View;

import Model.Difficulty;
import Model.GameRules;
import Model.PlayerColor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * New-game settings (FR-01 to FR-03). The view only collects the input;
 * validation happens in Model.SetupValidator and errors come back through {@link #showError}.
 */
public final class GameSetupView extends JPanel {
    private static final long serialVersionUID = 1L;

    @FunctionalInterface
    public interface StartListener {
        /** Notify the Controller with the chosen game settings. */
        void onStart(Difficulty difficulty, List<String> names, List<PlayerColor> colors);
    }

    private static final String NO_COLOR = "בחרו צבע";

    private final JComboBox<Integer> playerCount = new JComboBox<>();
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JTextField[] names = new JTextField[GameRules.MAX_PLAYERS];
    private final List<JComboBox<Object>> colors = new ArrayList<>();
    private final JPanel[] rows = new JPanel[GameRules.MAX_PLAYERS];

    /** Build the new-game configuration form and callbacks. */
    public GameSetupView(StartListener onStart, Runnable onBack) {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("הגדרת משחק חדש", 28, true), BorderLayout.LINE_START);
        JButton back = Theme.button("חזרה", false);
        back.addActionListener(e -> onBack.run());
        header.add(back, BorderLayout.LINE_END);
        add(header, BorderLayout.NORTH);

        JPanel card = Theme.card(new BorderLayout(0, 14));
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING, 14, 4));
        options.setOpaque(false);
        for (int n = GameRules.MIN_PLAYERS; n <= GameRules.MAX_PLAYERS; n++) playerCount.addItem(n);
        playerCount.setFont(Theme.font(15, false));
        difficulty.setFont(Theme.font(15, false));
        options.add(Theme.label("מספר שחקנים:", 15, true));
        options.add(playerCount);
        options.add(Box.createHorizontalStrut(20));
        options.add(Theme.label("רמת קושי:", 15, true));
        options.add(difficulty);
        JLabel levelNote = Theme.label("(באיטרציה 1 זמינה רמה קלה בלבד)", 13, false);
        levelNote.setForeground(Theme.MUTED);
        options.add(levelNote);
        card.add(options, BorderLayout.NORTH);

        JPanel players = new JPanel();
        players.setLayout(new BoxLayout(players, BoxLayout.Y_AXIS));
        players.setOpaque(false);
        for (int i = 0; i < GameRules.MAX_PLAYERS; i++) {
            players.add(createPlayerRow(i));
        }
        JPanel playersHolder = new JPanel(new BorderLayout());
        playersHolder.setOpaque(false);
        playersHolder.add(players, BorderLayout.NORTH);
        card.add(playersHolder, BorderLayout.CENTER);

        JLabel hint = Theme.label("לכל שחקן יש להזין שם או כינוי ולבחור צבע ייחודי.", 13, false);
        hint.setForeground(Theme.MUTED);
        card.add(hint, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEADING));
        footer.setOpaque(false);
        JButton start = Theme.button("התחלת משחק", true);
        start.addActionListener(e -> onStart.onStart(
                (Difficulty) difficulty.getSelectedItem(), selectedNames(), selectedColors()));
        footer.add(start);
        add(footer, BorderLayout.SOUTH);

        playerCount.addActionListener(e -> updateVisibleRows());
        updateVisibleRows();
    }

    /** Present a validation failure without leaving the setup screen. */
    public void showError(String message) { Dialogs.warning(this, message); }

    /** Create the name and color controls for one player. */
    private JPanel createPlayerRow(int i) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Theme.SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        row.add(Theme.label("שחקן " + (i + 1), 15, true), BorderLayout.LINE_START);
        names[i] = Theme.textField();
        names[i].setPreferredSize(new Dimension(300, 36));
        names[i].setToolTipText("שם או כינוי (עד " + GameRules.MAX_NAME_LENGTH + " תווים)");
        row.add(names[i], BorderLayout.CENTER);
        JComboBox<Object> color = new JComboBox<>();
        color.addItem(NO_COLOR);
        for (PlayerColor c : PlayerColor.values()) color.addItem(c);
        color.setFont(Theme.font(15, false));
        color.setPreferredSize(new Dimension(130, 34));
        colors.add(color);
        row.add(color, BorderLayout.LINE_END);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        wrapper.add(row, BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        rows[i] = wrapper;
        return wrapper;
    }

    /** Return the number of currently selected players. */
    private int count() { return (Integer) playerCount.getSelectedItem(); }

    /** Show only the player rows required by the current count. */
    private void updateVisibleRows() {
        for (int i = 0; i < rows.length; i++) rows[i].setVisible(i < count());
        revalidate();
        repaint();
    }

    /** Collect the names of players enabled in the form. */
    private List<String> selectedNames() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count(); i++) result.add(names[i].getText());
        return result;
    }

    /** Collect the colors of players enabled in the form. */
    private List<PlayerColor> selectedColors() {
        List<PlayerColor> result = new ArrayList<>();
        for (int i = 0; i < count(); i++) {
            Object item = colors.get(i).getSelectedItem();
            result.add(item instanceof PlayerColor pc ? pc : null);
        }
        return result;
    }
}
