package View;

import javax.swing.*;
import java.awt.*;

/** Opening screen (SRS 3.1.1). History and question management arrive in iteration 3 (SDP 5.2.1). */
public final class MainMenuView extends JPanel {
    private static final long serialVersionUID = 1L;

    public MainMenuView(Runnable onNewGame, Runnable onRules, Runnable onExit) {
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        JPanel card = Theme.card(new BorderLayout(0, 18));
        card.setPreferredSize(new Dimension(480, 520));

        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 4));
        titles.setOpaque(false);
        JLabel title = Theme.label("לודו קואלה", 36, true);
        JLabel subtitle = Theme.label("משחק Ludo לימודי בהנדסת תוכנה", 16, false);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        titles.add(title);
        titles.add(subtitle);
        card.add(titles, BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 10));
        actions.setOpaque(false);
        JButton newGame = Theme.button("משחק חדש", true);
        JButton rules = Theme.button("חוקי המשחק", false);
        JButton history = Theme.button("היסטוריית משחקים (באיטרציה 3)", false);
        JButton questions = Theme.button("ניהול שאלות (באיטרציה 3)", false);
        JButton exit = Theme.button("יציאה", false);
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

        JLabel footer = Theme.label("קבוצת Koala | איטרציה 1", 12, false);
        footer.setForeground(Theme.MUTED);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        add(card);
    }
}
