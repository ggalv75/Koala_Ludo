package View;

import Model.GameResult;

import javax.swing.*;
import java.awt.*;

/** Results screen (FR-49): ranking, difficulty and game duration; game actions are no longer possible. */
public final class ResultsView extends JPanel {
    private static final long serialVersionUID = 1L;

    /** Build the final ranking screen with colored koala avatars. */
    public ResultsView(GameResult result, Runnable onMainMenu) {
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        JPanel card = Theme.card(new BorderLayout(0, 18));
        card.setPreferredSize(new Dimension(520, 460));
        JLabel title = Theme.label("תוצאות המשחק", 30, true);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, BorderLayout.NORTH);

        JPanel places = new JPanel(new GridLayout(0, 1, 0, 10));
        places.setOpaque(false);
        for (GameResult.Standing standing : result.standings()) {
            // FR-47: the player left last simply takes the last place; it is a loss, not "did not finish".
            String text = "מקום " + standing.rank() + ":  " + standing.name();
            JLabel row = Theme.label(text, standing.rank() == 1 ? 22 : 19, standing.rank() == 1);
            row.setIcon(KoalaPiecePainter.icon(standing.color(), 28));
            row.setIconTextGap(10);
            row.setHorizontalAlignment(SwingConstants.CENTER);
            places.add(row);
        }
        card.add(places, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 12));
        bottom.setOpaque(false);
        JLabel info = Theme.label("רמת קושי: " + result.difficulty().displayName()
                + "   |   משך המשחק: " + result.formattedDuration(), 15, false);
        info.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(info);
        JButton menu = Theme.button("חזרה למסך הפתיחה", true);
        menu.addActionListener(e -> onMainMenu.run());
        bottom.add(menu);
        card.add(bottom, BorderLayout.SOUTH);
        add(card);
    }
}
