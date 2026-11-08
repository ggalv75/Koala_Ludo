package View;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.GameResult;
import Model.PlayerColor;

class ResultsViewTest {

    @Test
    void displaysRankingInOrder() throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            ResultsView view = new ResultsView(
                    createResult(3, 65), () -> { });

            List<String> ranking = findComponents(view, JLabel.class)
                    .stream()
                    .map(JLabel::getText)
                    .filter(text -> text.startsWith("מקום "))
                    .toList();

            assertEquals(List.of(
                    "מקום 1:  שחקן 1",
                    "מקום 2:  שחקן 2",
                    "מקום 3:  שחקן 3"
            ), ranking);
        });
    }

    /** FR-47: every player (2-4) gets a ranking row with an avatar. */
    @Test
    void displaysEveryPlayerWithAnAvatar() throws Exception {
        for (int playerCount = 2; playerCount <= 4; playerCount++) {
            assertRankingRowsWithAvatars(playerCount);
        }
    }

    /** The results screen shows the level and the formatted game duration. */
    @Test
    void displaysDifficultyAndDuration() throws Exception {
        assertDifficultyAndDuration(0, "00:00:00");
        assertDifficultyAndDuration(65, "00:01:05");
        assertDifficultyAndDuration(3661, "01:01:01");
    }

    private void assertRankingRowsWithAvatars(int playerCount)
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            ResultsView view = new ResultsView(
                    createResult(playerCount, 65), () -> { });

            List<JLabel> rankingRows =
                    findComponents(view, JLabel.class)
                            .stream()
                            .filter(label ->
                                    label.getText().startsWith("מקום "))
                            .toList();

            assertEquals(playerCount, rankingRows.size(),
                    "צריכה להופיע שורה אחת לכל שחקן");

            for (JLabel row : rankingRows) {
                assertNotNull(row.getIcon(),
                        "לכל שחקן צריך להופיע סמל קואלה");
            }
        });
    }

    private void assertDifficultyAndDuration(
            long seconds, String expectedDuration) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            GameResult result = createResult(2, seconds);
            ResultsView view = new ResultsView(result, () -> { });

            List<String> infoRows =
                    findComponents(view, JLabel.class)
                            .stream()
                            .map(JLabel::getText)
                            .filter(text ->
                                    text.startsWith("רמת קושי: "))
                            .toList();

            assertEquals(List.of(
                    "רמת קושי: " + result.difficulty().displayName()
                    + "   |   משך המשחק: " + expectedDuration
            ), infoRows);
        });
    }

    @Test
    void menuButtonInvokesCallbackOnceWhenClicked()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            int[] calls = {0};

            ResultsView view = new ResultsView(
                    createResult(2, 65),
                    () -> calls[0]++
            );

            List<JButton> buttons =
                    findComponents(view, JButton.class);

            assertEquals(1, buttons.size(),
                    "במסך התוצאות צריך להיות כפתור חזרה אחד");

            JButton menuButton = buttons.get(0);

            assertEquals("חזרה למסך הפתיחה", menuButton.getText());
            assertTrue(menuButton.isEnabled());
            assertEquals(0, calls[0],
                    "יצירת המסך לא אמורה להפעיל חזרה לתפריט");

            menuButton.doClick(0);

            assertEquals(1, calls[0],
                    "לחיצה אחת צריכה להפעיל את החזרה פעם אחת");
        });
    }

    private GameResult createResult(int playerCount, long seconds) {
        List<GameResult.Standing> standings = new ArrayList<>();
        PlayerColor[] colors = PlayerColor.values();

        for (int i = 0; i < playerCount; i++) {
            standings.add(new GameResult.Standing(
                    i + 1,
                    "שחקן " + (i + 1),
                    colors[i],
                    i < playerCount - 1
            ));
        }

        return new GameResult(
                Difficulty.EASY,
                standings,
                seconds,
                LocalDateTime.of(2026, 11, 7, 12, 0)
        );
    }

    private <T extends Component> List<T> findComponents(
            Container container, Class<T> type) {

        List<T> matches = new ArrayList<>();

        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }

            if (component instanceof Container childContainer) {
                matches.addAll(findComponents(childContainer, type));
            }
        }

        return matches;
    }
}