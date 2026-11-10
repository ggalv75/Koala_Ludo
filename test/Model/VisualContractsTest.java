package Model;

import View.BoardCanvas;
import View.DieFace;
import View.KoalaPiecePainter;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/** Regression checks for previously uncovered koala badges, die input, and selection pulse. */
public final class VisualContractsTest {
    private static int passed;

    /** Prevent instantiation of the stand-alone regression suite. */
    private VisualContractsTest() { }

    /** Reject a failed condition with a descriptive reason. */
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Confirm each numbered koala has a visibly different number badge. */
    private static void everyPieceNumberHasDistinctBadge() {
        int[][] rendered = new int[4][];
        for (int number = 1; number <= 4; number++) {
            BufferedImage image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                KoalaPiecePainter.draw(graphics, PlayerColor.GREEN, 75, 75, 53, number);
            } finally {
                graphics.dispose();
            }
            rendered[number - 1] = image.getRGB(0, 0, 160, 160, null, 0, 160);
        }
        for (int a = 0; a < rendered.length; a++) {
            for (int b = a + 1; b < rendered.length; b++) {
                check(!Arrays.equals(rendered[a], rendered[b]),
                        "Different koala piece numbers must produce distinct badge images: " + (a + 1) + "," + (b + 1));
            }
        }
        passed++;
        System.out.println("PASS unique numbered koala badges (1..4)");
    }

    /** Ensure only the six die results and the initial blank value are accepted. */
    private static void dieRejectsImpossibleDisplayValues() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DieFace die = new DieFace();
            for (int valid = 0; valid <= 6; valid++) die.setValue(valid);
            for (int invalid : new int[]{-1, 7, 99}) {
                boolean rejected = false;
                try {
                    die.setValue(invalid);
                } catch (IllegalArgumentException expected) {
                    rejected = true;
                }
                check(rejected, "Invalid die value must be rejected: " + invalid);
            }
        });
        passed++;
        System.out.println("PASS die validates display values 0..6");
    }

    /** Prevent Swing animations from starting outside its event-dispatch thread. */
    private static void dieAnimationRejectsOffEdtCalls() {
        DieFace die = new DieFace();
        boolean rejected = false;
        try {
            die.animateRoll(() -> { });
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        check(rejected, "Starting die animation off the EDT must be rejected");
        check(!die.isRolling(), "Invalid animation start must leave the die idle");
        passed++;
        System.out.println("PASS die animation is Swing-EDT safe");
    }

    /** Render the current board with the same Java2D logic used by the application. */
    private static BufferedImage render(BoardCanvas canvas) {
        BufferedImage image = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            canvas.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /** Verify legal-piece highlights pulse only with animations enabled. */
    private static void highlightedPiecePulsesAndReducedMotionIsStable() throws Exception {
        Dice fixedSix = new Dice(new Random(13)) {
            /** Supply the same opening six without relying on random luck. */
            @Override public DiceRoll roll() { return new DiceRoll(6); }
        };
        GameEngine engine = new GameEngine(Difficulty.EASY, List.of("Koala", "Panda"),
                List.of(PlayerColor.RED, PlayerColor.BLUE), new Random(7), fixedSix, Clock.systemUTC());
        engine.rollDice();
        check(!engine.state().legalMoves().isEmpty(), "Opening six must have legal pieces to highlight");
        AtomicReference<BoardCanvas> canvasRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            BoardCanvas canvas = new BoardCanvas(id -> { });
            canvas.setSize(640, 640);
            canvas.setState(engine.state());
            canvas.setAnimationsEnabled(false);
            canvasRef.set(canvas);
        });
        BoardCanvas canvas = canvasRef.get();
        AtomicReference<BufferedImage> first = new AtomicReference<>();
        AtomicReference<BufferedImage> second = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> first.set(render(canvas)));
            Thread.sleep(110);
            SwingUtilities.invokeAndWait(() -> second.set(render(canvas)));
            check(Arrays.equals(first.get().getRGB(0, 0, 640, 640, null, 0, 640),
                    second.get().getRGB(0, 0, 640, 640, null, 0, 640)),
                    "Reduced-motion highlight must have a stable appearance");
            SwingUtilities.invokeAndWait(() -> canvas.setAnimationsEnabled(true));
            boolean differenceObserved = false;
            for (int i = 0; i < 6 && !differenceObserved; i++) {
                SwingUtilities.invokeAndWait(() -> first.set(render(canvas)));
                Thread.sleep(115);
                SwingUtilities.invokeAndWait(() -> second.set(render(canvas)));
                differenceObserved = !Arrays.equals(first.get().getRGB(0, 0, 640, 640, null, 0, 640),
                        second.get().getRGB(0, 0, 640, 640, null, 0, 640));
            }
            check(differenceObserved, "Legal-piece highlighting must pulse across animation frames");
        } finally {
            SwingUtilities.invokeAndWait(canvas::stopEffects);
        }
        passed++;
        System.out.println("PASS selected-piece pulse and reduced-motion fallback");
    }

    /** Run additional visual contract checks and summarize their outcome. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        everyPieceNumberHasDistinctBadge();
        dieRejectsImpossibleDisplayValues();
        dieAnimationRejectsOffEdtCalls();
        highlightedPiecePulsesAndReducedMotionIsStable();
        System.out.printf("RESULT VisualContractsTest: %d/4 passed%n", passed);
    }
}
