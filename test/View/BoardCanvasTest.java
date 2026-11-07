package View;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.GameEngine;
import Model.GameState;
import Model.Piece;
import Model.Player;
import Model.PlayerColor;
import Model.PlayerFinishEvent;
import Model.Position;

class BoardCanvasTest {

    private BoardCanvas canvas;
    private GameState state;
    private List<Integer> clickedPieceIds;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PlayerColor[] colors = PlayerColor.values();

            GameEngine engine = new GameEngine(
                    Difficulty.EASY,
                    List.of("דור", "גל"),
                    List.of(colors[0], colors[1])
            );

            state = engine.state();
            clickedPieceIds = new ArrayList<>();

            canvas = new BoardCanvas(clickedPieceIds::add);
            canvas.setSize(640, 640);
            canvas.setState(state);

            paintCanvas();
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (canvas != null) {
                canvas.stopEffects();
            }
        });
    }

    @Test
    void clickingPieceReportsItsIdWithoutMovingIt()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            Piece piece = state.pieces().get(0);

            assertTrue(piece.isInCastle());

            clickAt(castlePieceCenter(piece));

            assertEquals(List.of(piece.id()), clickedPieceIds,
                    "לחיצה צריכה להעביר את מזהה הכלי");

            assertTrue(piece.isInCastle(),
                    "התצוגה צריכה להעביר את הבחירה לבקר");

            assertEquals(GameState.Phase.WAITING_FOR_ROLL,
                    state.phase());
        });
    }

    @Test
    void clickingOutsideBoardDoesNotReportPiece()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            clickAt(new Point(0, 0));

            assertTrue(clickedPieceIds.isEmpty(),
                    "לחיצה בשולי הרכיב לא אמורה לבחור כלי");
        });
    }

    @Test
    void clicksAreIgnoredDuringAnimation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Piece piece = state.pieces().get(0);

            startFinishAnimation();

            assertTrue(canvas.isBusy());

            paintCanvas();
            clickAt(castlePieceCenter(piece));

            assertTrue(clickedPieceIds.isEmpty(),
                    "לחיצה בזמן אנימציה צריכה להיחסם");
        });
    }

    @Test
    void continuationRunsImmediatelyWhenCanvasIsIdle()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger calls = new AtomicInteger();

            assertFalse(canvas.isBusy());

            canvas.whenEffectsFinish(calls::incrementAndGet);

            assertEquals(1, calls.get(),
                    "כשהלוח פנוי, פעולת ההמשך צריכה לרוץ מיד");
        });
    }

    @Test
    void disablingAnimationsCompletesPendingContinuationOnce()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger calls = new AtomicInteger();

            startFinishAnimation();
            assertTrue(canvas.isBusy());

            canvas.whenEffectsFinish(calls::incrementAndGet);

            assertEquals(0, calls.get(),
                    "פעולת ההמשך צריכה להמתין לאנימציה");

            canvas.setAnimationsEnabled(false);

            assertFalse(canvas.isBusy());
            assertEquals(1, calls.get());

            canvas.setAnimationsEnabled(false);
            canvas.stopEffects();

            assertEquals(1, calls.get(),
                    "פעולת ההמשך צריכה להתבצע פעם אחת בלבד");
        });
    }

    @Test
    void stoppingEffectsDiscardsPendingContinuation()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger calls = new AtomicInteger();

            startFinishAnimation();
            canvas.whenEffectsFinish(calls::incrementAndGet);

            assertTrue(canvas.isBusy());
            assertEquals(0, calls.get());

            canvas.stopEffects();

            assertFalse(canvas.isBusy());

            // שינוי ההגדרה לאחר העצירה לא אמור לשחרר פעולה ישנה.
            canvas.setAnimationsEnabled(false);

            assertEquals(0, calls.get(),
                    "יציאה מהמסך צריכה לבטל פעולות המשך ממתינות");
        });
    }

    @Test
    void animationNaturallyFinishesAndRunsContinuationOnce()
            throws Exception {

        CountDownLatch finished = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            startFinishAnimation();

            assertTrue(canvas.isBusy());

            canvas.whenEffectsFinish(() -> {
                calls.incrementAndGet();
                finished.countDown();
            });

            assertEquals(0, calls.get());
        });

        assertTrue(finished.await(7, TimeUnit.SECONDS),
                "האנימציה צריכה להסתיים בתוך שבע שניות");

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(canvas.isBusy());
            assertEquals(1, calls.get());

            canvas.setAnimationsEnabled(false);
            canvas.stopEffects();

            assertEquals(1, calls.get(),
                    "סיום האנימציה צריך להפעיל את ההמשך פעם אחת");
        });
    }

    private void startFinishAnimation() {
        Player player = state.currentPlayer();

        PlayerFinishEvent event = new PlayerFinishEvent(
                player.index(),
                player.name(),
                player.color(),
                1
        );

        canvas.setState(state, null, null, event);
    }

    private void paintCanvas() {
        BufferedImage image = new BufferedImage(
                canvas.getWidth(),
                canvas.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = image.createGraphics();

        try {
            canvas.paint(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private Point castlePieceCenter(Piece piece) {
        int boardSize = state.board().size();

        int cell = Math.max(
                10,
                Math.min(
                        (canvas.getWidth() - 20) / boardSize,
                        (canvas.getHeight() - 20) / boardSize
                )
        );

        int originX =
                (canvas.getWidth() - cell * boardSize) / 2;
        int originY =
                (canvas.getHeight() - cell * boardSize) / 2;

        Position corner =
                state.board().castleCorner(piece.color());

        int slot = piece.number() - 1;

        int x = (int) (
                originX + corner.col() * cell
                + cell * (1.9 + (slot % 2) * 2.2)
        );

        int y = (int) (
                originY + corner.row() * cell
                + cell * (1.9 + (slot / 2) * 2.2)
        );

        return new Point(x, y);
    }

    private void clickAt(Point point) {
        canvas.dispatchEvent(new MouseEvent(
                canvas,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                point.x,
                point.y,
                1,
                false,
                MouseEvent.BUTTON1
        ));
    }
}