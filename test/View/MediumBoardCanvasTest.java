package View;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import Model.*;

class MediumBoardCanvasTest {
    @Test
    void mediumCenterHasCorrectColors() throws Exception {
        checkCenterColors(Difficulty.MEDIUM, PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE);
    }

    @Test
    void easyCenterKeepsOriginalColors() throws Exception {
        checkCenterColors(Difficulty.EASY, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE, PlayerColor.RED);
    }

    private void checkCenterColors(Difficulty difficulty, PlayerColor left, PlayerColor top, PlayerColor right, PlayerColor bottom) throws Exception {
        Board board = BoardFactory.create(difficulty, new Random(42));
        BufferedImage image = new BufferedImage(700, 700, BufferedImage.TYPE_INT_RGB);

        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(difficulty, List.of("Red", "Green"), List.of(PlayerColor.RED, PlayerColor.GREEN));
            BoardCanvas canvas = new BoardCanvas(id -> {});
            canvas.setSize(700, 700);
            canvas.setState(engine.state());
            Graphics2D g = image.createGraphics();
            canvas.paint(g);
            g.dispose();
            canvas.stopEffects();
        });

        int cell = (700 - 20) / board.size();
        int origin = (700 - cell * board.size()) / 2;
        int x = origin + board.centerCorner().col() * cell;
        int y = origin + board.centerCorner().row() * cell;
        int d = cell * 3;

        assertEquals(Theme.boardPathColorOf(left).getRGB(), image.getRGB(x + cell / 3, y + d / 2));
        assertEquals(Theme.boardPathColorOf(top).getRGB(), image.getRGB(x + d / 2, y + cell / 3));
        assertEquals(Theme.boardPathColorOf(right).getRGB(), image.getRGB(x + d - cell / 3, y + d / 2));
        assertEquals(Theme.boardPathColorOf(bottom).getRGB(), image.getRGB(x + d / 2, y + d - cell / 3));
    }
}