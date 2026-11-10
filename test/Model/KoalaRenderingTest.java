package Model;

import View.BoardCanvas;
import View.KoalaPiecePainter;
import View.Theme;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tests the koala rendering without making visual styling part of the game rules. */
public final class KoalaRenderingTest {
    /** Run image, icon and state-isolation checks, then render two preview images. */
    public static void main(String[] args) throws Exception {
        Path directory = Path.of("docs");
        Files.createDirectories(directory);
        int cases = 0;
        Set<Integer> uniqueColorPixels = new HashSet<>();
        for (PlayerColor color : PlayerColor.values()) {
            BufferedImage image = new BufferedImage(170, 170, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            KoalaPiecePainter.draw(graphics, color, 85, 85, 55, 2);
            graphics.dispose();
            int painted = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) > 0) painted++;
                }
            }
            require(painted > 1200, "Colored koala has a visible silhouette: " + color);
            uniqueColorPixels.add(image.getRGB(57, 88));
            cases++;
            Icon avatar = KoalaPiecePainter.icon(color, 26);
            require(avatar.getIconWidth() == 26 && avatar.getIconHeight() == 26,
                    "Sidebar icons preserve size for " + color);
            cases++;
        }
        require(uniqueColorPixels.size() == 4, "The four player colors must remain distinguishable");
        cases++;

        BufferedImage tiny = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D small = tiny.createGraphics();
        KoalaPiecePainter.draw(small, PlayerColor.BLUE, 12, 12, 7, 1);
        small.dispose();
        require(tiny.getRGB(12, 12) >>> 24 != 0, "Tiny finish-area koalas remain visible");
        cases++;

        GameEngine engine = new GameEngine(Difficulty.EASY,
                List.of("Red", "Green", "Yellow", "Blue"),
                List.of(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE));
        GameState state = engine.state();
        int finish = state.board().finishStep();
        state.piece(0).moveTo(5);
        state.piece(1).moveTo(19);
        state.piece(2).moveTo(finish);
        state.piece(4).moveTo(22);
        state.piece(8).moveTo(38);
        state.piece(12).moveTo(45);
        state.setLegalMoves(List.of(new Move(0, 5, 11, false, false)));
        BoardCanvas canvas = new BoardCanvas(id -> { });
        canvas.setSize(850, 850);
        canvas.setState(state);
        BufferedImage board = new BufferedImage(850, 850, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = board.createGraphics();
        canvas.paint(g);
        g.dispose();
        require(state.piece(0).step() == 5 && state.piece(2).step() == finish,
                "Drawing cannot mutate Model piece positions");
        require(board.getRGB(425, 425) >>> 24 != 0, "The model board paints successfully");
        ImageIO.write(board, "png", directory.resolve("KoalaBoard_preview.png").toFile());
        cases++;
        System.out.println("PASS: Koala rendering " + cases + "/" + cases);
        createAvatarPreview(directory.resolve("KoalaColors_preview.png"));
    }

    /** Render a high-resolution view of the four numbered koala heads. */
    private static void createAvatarPreview(Path file) throws Exception {
        BufferedImage image = new BufferedImage(1120, 390, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(246, 247, 251));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(Theme.INK);
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 32));
        g.drawString("Koala Ludo – Color-coded pieces", 250, 60);
        PlayerColor[] colors = PlayerColor.values();
        for (int i = 0; i < colors.length; i++) {
            int x = 140 + i * 280;
            g.setColor(Color.WHITE);
            g.fillRoundRect(x - 116, 80, 235, 270, 24, 24);
            KoalaPiecePainter.draw(g, colors[i], x, 198, 83, i + 1);
            g.setColor(Theme.INK);
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
            g.drawString(colors[i].name(), x - 44, 325);
        }
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    /** Fail an executable smoke test when a rendering invariant is violated. */
    private static void require(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
