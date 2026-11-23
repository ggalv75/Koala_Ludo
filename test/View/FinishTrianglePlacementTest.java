package View;

import static org.junit.jupiter.api.Assertions.*;

import Model.*;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Finished pawns must appear in the triangle connected to their home path. */
class FinishTrianglePlacementTest {

    static Stream<Arguments> cases() {
        return Arrays.stream(Difficulty.values())
                .flatMap(difficulty ->
                        Arrays.stream(PlayerColor.values())
                                .map(color ->
                                        Arguments.of(
                                                difficulty,
                                                color)));
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("cases")
    void finishedPawnMatchesItsHomePath(
            Difficulty difficulty,
            PlayerColor color) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            BoardCanvas canvas =
                    new BoardCanvas(id -> { });

            try {
                GameEngine engine = new GameEngine(
                        difficulty,
                        List.of("Red", "Green"),
                        List.of(
                                PlayerColor.RED,
                                PlayerColor.GREEN));

                Board board = engine.state().board();

                canvas.setAnimationsEnabled(false);
                canvas.setState(engine.state());
                canvas.setSize(640, 640);

                BufferedImage image = new BufferedImage(
                        640,
                        640,
                        BufferedImage.TYPE_INT_ARGB);

                var graphics = image.createGraphics();
                canvas.paint(graphics);
                graphics.dispose();

                var cellField = BoardCanvas.class
                        .getDeclaredField("cell");

                var xField = BoardCanvas.class
                        .getDeclaredField("originX");

                var yField = BoardCanvas.class
                        .getDeclaredField("originY");

                cellField.setAccessible(true);
                xField.setAccessible(true);
                yField.setAccessible(true);

                int cell = cellField.getInt(canvas);
                Position center = board.centerCorner();

                double centerX = xField.getInt(canvas)
                        + cell * (center.col() + 1.5);

                double centerY = yField.getInt(canvas)
                        + cell * (center.row() + 1.5);

                Position lastHome = board.positionOf(
                        color,
                        board.finishStep() - 1);

                int homeDx = lastHome.col()
                        - (center.col() + 1);

                int homeDy = lastHome.row()
                        - (center.row() + 1);

                Method finishSpot =
                        BoardCanvas.class.getDeclaredMethod(
                                "finishSpot",
                                Board.class,
                                PlayerColor.class,
                                int.class);

                finishSpot.setAccessible(true);

                for (int slot = 0;
                        slot < GameRules.PIECES_PER_PLAYER;
                        slot++) {

                    Point point = (Point) finishSpot.invoke(
                            canvas,
                            board,
                            color,
                            slot);

                    double dx = point.x - centerX;
                    double dy = point.y - centerY;

                    boolean correctTriangle =
                            Math.abs(homeDx) > Math.abs(homeDy)
                                    ? dx * homeDx > 0
                                        && Math.abs(dx) > Math.abs(dy)
                                    : dy * homeDy > 0
                                        && Math.abs(dy) > Math.abs(dx);

                    assertTrue(
                            correctTriangle,
                            difficulty + " / " + color
                                    + " / slot " + slot
                                    + ": finished pawn is outside "
                                    + "its own triangle");
                }

            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);

            } finally {
                canvas.stopEffects();
            }
        });
    }
}