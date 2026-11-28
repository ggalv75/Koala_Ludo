package Model;

import static org.junit.jupiter.api.Assertions.*;

import View.BoardCanvas;
import View.GameBoardView;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the Observer connection from the Engine to the View
 * using a controlled board.
 */
class SurpriseViewIntegrationTest {

    private GameEngine engine;
    private GameBoardView view;

    @AfterEach
    void closeView() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (view != null) {
                view.stopClock();
            }
        });
    }

    @Test
    void observerDisplaysTheSurpriseNameWithoutAnimations()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            create(20, false);
            engine.movePiece(0);

            JLabel label = named(
                    view, JLabel.class, "surprise-info");

            assertTrue(label.getText().contains(
                    SurpriseType.RETURN_TO_CASTLE.displayName()));

            assertTrue(label.getText().contains("דור"));
            assertTrue(label.getParent().isVisible());
            assertTrue(engine.state().piece(0).isInCastle());
        });
    }

    @Test
    void opponentSelectionShowsInstructionsAndForwardsTheClickedPawn()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            create(10, false);
            engine.state().piece(4).moveTo(10);
            engine.movePiece(0);

            assertTrue(named(view, JLabel.class, "surprise-info")
                    .getText().contains("בחר כלי יריב"));

            assertFalse(named(view, JButton.class, "roll-button")
                    .isEnabled());

            BoardCanvas canvas = canvas();
            canvas.setSize(640, 640);

            BufferedImage image = new BufferedImage(
                    640, 640, BufferedImage.TYPE_INT_ARGB);

            var graphics = image.createGraphics();
            canvas.paint(graphics);
            graphics.dispose();

            clickDrawnPawn(canvas, 4);

            assertEquals(7, engine.state().piece(4).step());
            assertEquals(1, engine.state().currentPlayerIndex());

            assertTrue(named(view, JButton.class, "roll-button")
                    .isEnabled());
        });
    }

    @Test
    void firstMovementIsRetainedAndVisualSnapshotsDoNotChangeTheModel()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            create(20, true);
            engine.movePiece(0);

            assertTrue(engine.state().piece(0).isInCastle());

            Object motion = field(canvas(), "motion");
            assertNotNull(motion);

            Move firstMove = (Move) accessor(motion, "move");

            assertEquals(5, firstMove.fromStep());
            assertEquals(6, firstMove.toStep());

            Map<?, ?> visualSteps =
                    (Map<?, ?>) field(canvas(), "displayedSteps");

            assertEquals(6, visualSteps.get(0));

            assertTrue(engine.state().piece(0).isInCastle());

            assertFalse(named(view, JButton.class, "roll-button")
                    .isEnabled());
        });
    }

    @Test
    void completionWaitsForTheMovementSurpriseAndFollowingMovement()
            throws Exception {

        CountDownLatch finished = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            create(30, true);
            engine.movePiece(0);

            view.whenVisualEffectsFinish(finished::countDown);

            Move firstMove = (Move) accessor(
                    field(canvas(), "motion"), "move");

            assertEquals(6, firstMove.toStep());
            assertEquals(1L, finished.getCount());
        });

        assertTrue(finished.await(7, TimeUnit.SECONDS),
                "The complete sequence must finish, "
                        + "not only its first movement");

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(3, engine.state().piece(0).step());
            assertFalse(canvas().isBusy());

            assertNull(field(view, "surpriseTimer"));

            assertFalse((Boolean) field(
                    view, "visualSequenceRunning"));

            assertTrue(named(view, JButton.class, "roll-button")
                    .isEnabled());

            assertTrue(named(view, JLabel.class, "surprise-info")
                    .getText().contains(
                            SurpriseType.MOVE_BACKWARD_THREE
                                    .displayName()));
        });
    }

    @Test
    void disablingAnimationsCompletesTheWholePendingSequenceOnce()
            throws Exception {

        AtomicInteger completions = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            create(20, true);
            engine.movePiece(0);

            view.whenVisualEffectsFinish(
                    completions::incrementAndGet);

            named(view, JCheckBox.class, "animations-toggle")
                    .doClick(0);

            assertEquals(1, completions.get());
            assertFalse(canvas().isBusy());
            assertNull(field(view, "surpriseTimer"));

            assertTrue(named(view, JButton.class, "roll-button")
                    .isEnabled());

            assertTrue(named(view, JLabel.class, "surprise-info")
                    .getText().contains(
                            SurpriseType.RETURN_TO_CASTLE
                                    .displayName()));
        });
    }

    @Test
    void navigatingAwayDiscardsThePendingCompletionCallback()
            throws Exception {

        AtomicInteger completions = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            create(30, true);
            engine.movePiece(0);

            view.whenVisualEffectsFinish(
                    completions::incrementAndGet);

            view.stopClock();
            canvas().setAnimationsEnabled(false);

            assertEquals(0, completions.get());
            assertFalse(canvas().isBusy());

            assertFalse((Boolean) field(
                    view, "visualSequenceRunning"));

            assertNull(field(view, "surpriseTimer"));

            assertFalse(((javax.swing.Timer) field(
                    canvas(), "effectsTimer")).isRunning());
        });
    }

    private void create(int ticket, boolean animations) {
        Board base = BoardFactory.create(
                Difficulty.EASY, new Random(1));

        Map<Position, Cell> cells =
                new LinkedHashMap<>(base.cells());

        cells.put(
                base.positionOf(PlayerColor.RED, 6),
                new Cell(CellType.SURPRISE, null));

        cells.put(
                base.positionOf(PlayerColor.RED, 3),
                new Cell(CellType.REGULAR, null));

        Map<PlayerColor, List<Position>> routes =
                new EnumMap<>(PlayerColor.class);

        Map<PlayerColor, Position> castles =
                new EnumMap<>(PlayerColor.class);

        for (PlayerColor color : PlayerColor.values()) {
            routes.put(color, base.route(color));
            castles.put(color, base.castleCorner(color));
        }

        Board board = new Board(
                base.difficulty(),
                cells,
                routes,
                castles,
                base.centerCorner());

        Dice dice = new Dice(new Random(2)) {
            @Override
            public DiceRoll roll() {
                return new DiceRoll(5);
            }
        };

        Random draw = new Random() {
            private static final long serialVersionUID = 1L;

            @Override
            public int nextInt(int bound) {
                assertEquals(100, bound);
                return ticket;
            }
        };

        engine = new GameEngine(
                board,
                List.of("דור", "גל"),
                List.of(PlayerColor.RED, PlayerColor.GREEN),
                dice,
                Clock.systemUTC(),
                new SurpriseSelector(draw));

        engine.state().piece(0).moveTo(5);
        engine.state().setLastRoll(new DiceRoll(1));

        engine.state().setLegalMoves(
                List.of(new Move(0, 5, 6, false, false)));

        engine.state().setPhase(
                GameState.Phase.WAITING_FOR_MOVE);

        view = new GameBoardView(
                engine::elapsedSeconds,
                engine::rollDice,
                engine::movePiece,
                () -> { });

        view.onStateChanged(engine.state());
        engine.addListener(view);

        if (!animations) {
            named(view, JCheckBox.class, "animations-toggle")
                    .doClick(0);
        }
    }

    private BoardCanvas canvas() {
        return components(view, BoardCanvas.class).get(0);
    }

    private static void clickDrawnPawn(
            BoardCanvas canvas, int id) {

        List<?> pieces = (List<?>) field(
                canvas, "drawnPieces");

        for (Object piece : pieces) {
            if (((Integer) accessor(piece, "id")) == id) {
                int x = (Integer) accessor(piece, "x");
                int y = (Integer) accessor(piece, "y");

                canvas.dispatchEvent(new MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(),
                        0,
                        x,
                        y,
                        1,
                        false,
                        MouseEvent.BUTTON1));

                return;
            }
        }

        fail("The selectable opponent pawn must be drawn");
    }

    private static Object field(
            Object target, String name) {

        try {
            Field field = target.getClass()
                    .getDeclaredField(name);

            field.setAccessible(true);
            return field.get(target);

        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static Object accessor(
            Object target, String name) {

        try {
            Method method = target.getClass()
                    .getDeclaredMethod(name);

            method.setAccessible(true);
            return method.invoke(target);

        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static <T extends Component> T named(
            Container root, Class<T> type, String name) {

        return components(root, type).stream()
                .filter(component ->
                        name.equals(component.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static <T extends Component> List<T> components(
            Container root, Class<T> type) {

        List<T> result = new ArrayList<>();

        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                result.add(type.cast(child));
            }

            if (child instanceof Container container) {
                result.addAll(components(container, type));
            }
        }

        return result;
    }
}