package Model;

import View.BoardCanvas;
import View.GameBoardView;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Headless integration checks for interaction animations and authoritative state. */
public final class PieceInteractionVisualTest {
    private static int passed;

    /** Run code on Swing's event-dispatch thread. */
    private static void onEdt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }

    /** Return an engine with predictable random board and die output. */
    private static GameEngine engine(int roll) {
        Dice dice = new Dice(new Random(1)) {
            /** Return the selected value for this test. */
            @Override public DiceRoll roll() { return new DiceRoll(roll); }
        };
        return new GameEngine(Difficulty.EASY, List.of("A", "B"),
                List.of(PlayerColor.RED, PlayerColor.GREEN), new Random(23), dice, Clock.systemUTC());
    }

    /** Fail if a visual or game-state invariant does not hold. */
    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
    }

    /** Locate the board canvas nested inside the main game view. */
    private static BoardCanvas findCanvas(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof BoardCanvas canvas) return canvas;
            if (child instanceof Container nested) {
                BoardCanvas result = findCanvas(nested);
                if (result != null) return result;
            }
        }
        return null;
    }

    /** Render one real Swing frame into a headless bitmap. */
    private static BufferedImage render(BoardCanvas canvas) {
        BufferedImage image = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        canvas.paint(g);
        g.dispose();
        return image;
    }

    /** Check whether two rendered images differ visibly in at least one hundred pixels. */
    private static boolean visiblyDifferent(BufferedImage a, BufferedImage b) {
        int difference = 0;
        for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) {
            if (a.getRGB(x, y) != b.getRGB(x, y) && ++difference > 100) return true;
        }
        return false;
    }

    /** Translate an opponent pawn onto one shared physical square. */
    private static void putAt(GameState state, Piece piece, Position target) {
        int step = state.board().route(piece.color()).indexOf(target);
        check(step >= 0, "target on opponent track");
        piece.moveTo(step);
    }

    /** Validate capture, shield, block and reduced-motion effects on Swing. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        CountDownLatch captureDone = new CountDownLatch(1);
        AtomicReference<GameBoardView> captureBoard = new AtomicReference<>();
        AtomicReference<BoardCanvas> captureCanvas = new AtomicReference<>();
        AtomicReference<GameEngine> capturedEngine = new AtomicReference<>();
        onEdt(() -> {
            GameEngine e = engine(3);
            GameState s = e.state();
            int target = 6;
            while (s.board().cellAt(PlayerColor.RED, target).type() != CellType.REGULAR) target++;
            s.piece(0).moveTo(target - 3);
            putAt(s, s.piece(4), s.board().positionOf(PlayerColor.RED, target));
            GameBoardView board = new GameBoardView(e::elapsedSeconds, e::rollDice, e::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(board);
            canvas.setSize(640, 640);
            board.onStateChanged(s);
            e.addListener(board);
            e.rollDice();
            check(e.movePiece(0), "capture succeeds");
            check(s.piece(4).isInCastle(), "victim already in castle in the Model");
            check(canvas.isBusy(), "animation persists after capture");
            render(canvas);
            canvas.whenEffectsFinish(captureDone::countDown);
            captureBoard.set(board); captureCanvas.set(canvas); capturedEngine.set(e);
            System.out.println("PASS animated capture uses authoritative Model state");
            passed++;
        });
        check(captureDone.await(4, TimeUnit.SECONDS), "capture animation and callback finish");
        onEdt(() -> {
            check(!captureCanvas.get().isBusy(), "capture ghost cleared");
            check(capturedEngine.get().state().piece(4).isInCastle(), "victim remains captured");
            captureBoard.get().stopClock();
            System.out.println("PASS capture animation completes without changing the result");
            passed++;
        });

        CountDownLatch shieldDone = new CountDownLatch(1);
        AtomicReference<GameBoardView> shieldBoard = new AtomicReference<>();
        onEdt(() -> {
            GameEngine e = engine(6);
            GameState s = e.state();
            putAt(s, s.piece(4), s.board().positionOf(PlayerColor.RED, 0));
            GameBoardView board = new GameBoardView(e::elapsedSeconds, e::rollDice, e::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(board);
            canvas.setSize(640, 640);
            board.onStateChanged(s); e.addListener(board);
            e.rollDice(); check(e.movePiece(0), "shielded release");
            check(canvas.isBusy(), "shield interaction animation active");
            check(!s.piece(4).isInCastle(), "the protected opponent is not captured");
            canvas.whenEffectsFinish(shieldDone::countDown);
            shieldBoard.set(board);
            System.out.println("PASS protected landing visual effect preserves opponent");
            passed++;
        });
        check(shieldDone.await(4, TimeUnit.SECONDS), "shield animation completes");
        onEdt(() -> shieldBoard.get().stopClock());

        CountDownLatch blockDone = new CountDownLatch(1);
        AtomicReference<GameBoardView> blockBoard = new AtomicReference<>();
        onEdt(() -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            s.piece(0).moveTo(0); s.piece(1).moveTo(2);
            GameBoardView board = new GameBoardView(e::elapsedSeconds, e::rollDice, e::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(board);
            canvas.setSize(640, 640);
            board.onStateChanged(s); e.addListener(board);
            BufferedImage before = render(canvas);
            e.rollDice(); check(e.movePiece(0), "block-forming move");
            BufferedImage during = render(canvas);
            check(visiblyDifferent(before, during), "block formation changes rendered frame");
            check(InteractionRules.isDoubleBlock(s, s.positionOf(s.piece(0))), "block exists in model");
            canvas.whenEffectsFinish(blockDone::countDown);
            blockBoard.set(board);
            System.out.println("PASS Double Block effect follows model block creation");
            passed++;
        });
        check(blockDone.await(4, TimeUnit.SECONDS), "block effect completes");
        onEdt(() -> blockBoard.get().stopClock());

        CountDownLatch dissolveDone = new CountDownLatch(1);
        AtomicReference<GameBoardView> dissolvedBoard = new AtomicReference<>();
        onEdt(() -> {
            GameEngine e = engine(1);
            GameState s = e.state();
            s.piece(0).moveTo(2); s.piece(1).moveTo(2);
            GameBoardView board = new GameBoardView(e::elapsedSeconds, e::rollDice, e::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(board);
            canvas.setSize(640, 640);
            board.onStateChanged(s); e.addListener(board);
            e.rollDice(); check(e.movePiece(0), "block-dissolving move legal");
            check(!InteractionRules.isDoubleBlock(s, s.positionOf(s.piece(1))), "block dissolved in model");
            check(canvas.isBusy(), "dissolve visual started");
            canvas.whenEffectsFinish(dissolveDone::countDown);
            dissolvedBoard.set(board);
            System.out.println("PASS block dissolution animation preserves model state");
            passed++;
        });
        check(dissolveDone.await(4, TimeUnit.SECONDS), "dissolution animation completes");
        onEdt(() -> dissolvedBoard.get().stopClock());

        CountDownLatch blockedDone = new CountDownLatch(1);
        AtomicReference<GameBoardView> blockedBoard = new AtomicReference<>();
        onEdt(() -> {
            GameEngine e = engine(4);
            GameState s = e.state();
            s.piece(0).moveTo(2);
            Position wall = s.board().positionOf(PlayerColor.RED, 4);
            putAt(s, s.piece(4), wall); putAt(s, s.piece(5), wall);
            GameBoardView board = new GameBoardView(e::elapsedSeconds, e::rollDice, e::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(board);
            canvas.setSize(640, 640);
            board.onStateChanged(s); e.addListener(board);
            e.rollDice(); check(e.movePiece(0), "partial move legal");
            check(s.piece(0).step() == 3, "pawn stops one square before wall");
            check(canvas.isBusy(), "blocked-square feedback scheduled");
            canvas.whenEffectsFinish(blockedDone::countDown);
            blockedBoard.set(board);
            System.out.println("PASS partial-move blocker effect never bypasses a wall");
            passed++;
        });
        check(blockedDone.await(4, TimeUnit.SECONDS), "blocked effect completes");
        onEdt(() -> blockedBoard.get().stopClock());

        onEdt(() -> {
            GameEngine e = engine(6);
            BoardCanvas canvas = new BoardCanvas(id -> {});
            canvas.setSize(640, 640);
            e.rollDice();
            canvas.setState(e.state(), new Move(0, -1, 0, true, false),
                    new PieceInteractionEvent(null, e.state().board().positionOf(PlayerColor.RED, 0),
                            null, List.of()));
            check(canvas.isBusy(), "animation began");
            canvas.setAnimationsEnabled(false);
            check(!canvas.isBusy(), "reduced-motion toggle finishes immediately");
            render(canvas);
            canvas.stopEffects();
            System.out.println("PASS reduced-motion option disables interaction animations");
            passed++;
        });
        System.out.printf("RESULT PieceInteractionVisualTest: %d/%d passed%n", passed, 7);
    }
}
