package Model;

import View.BoardCanvas;
import View.GameBoardView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Headless Swing smoke tests for Piece Movement visualization. No GUI window is needed. */
public final class VisualEffectsSmokeTest {
    private static int passed;
    /** Create a deterministic game engine for the test scenario. */
    private static GameEngine engine(int... values) {
        Dice dice = new Dice(new Random()) {
            private int index;
            /** Produce the next scripted die result. */
            @Override public DiceRoll roll() { return new DiceRoll(values[index++]); }
        };
        return new GameEngine(Difficulty.EASY, List.of("Adi", "Dana"),
                List.of(PlayerColor.RED, PlayerColor.GREEN), new Random(3), dice, Clock.systemUTC());
    }

    /** Run the requested GUI test operation on the Swing event thread. */
    private static void edt(Runnable action) {
        try { SwingUtilities.invokeAndWait(action); }
        catch (Exception error) { throw new AssertionError(error); }
    }

    /** Assert a test expectation and provide a failure message. */
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** Render the board to a buffered image for visual assertions. */
    private static BufferedImage render(BoardCanvas canvas) {
        BufferedImage result = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        canvas.paint(g);
        g.dispose();
        return result;
    }

    /** Check that animation frames differ at the correct point. */
    private static boolean changed(BufferedImage a, BufferedImage b) {
        int changed = 0;
        for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) {
            if (a.getRGB(x, y) != b.getRGB(x, y)) changed++;
        }
        return changed > 100;
    }

    /** Locate the roll control in the test view. */
    private static JButton findRollButton(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof JButton button && "הטלת קובייה".equals(button.getText())) return button;
            if (child instanceof Container nested) {
                JButton found = findRollButton(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Run the test cases and report successful assertions. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        // Test 1: highlighting previews the actual legal castle release route.
        edt(() -> {
            GameEngine engine = engine(6);
            BoardCanvas canvas = new BoardCanvas(id -> {});
            canvas.setSize(640, 640);
            engine.rollDice();
            canvas.setState(engine.state());
            canvas.setAnimationsEnabled(false); // deterministic image comparison
            BufferedImage before = render(canvas);
            int cell = (640 - 20) / 15;
            int origin = (640 - cell * 15) / 2;
            Position castle = engine.state().board().castleCorner(engine.state().currentPlayer().color());
            int castleX = (int) (origin + cell * (castle.col() + 1.9));
            int castleY = (int) (origin + cell * (castle.row() + 1.9));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, castleX, castleY, 0, false));
            BufferedImage hovered = render(canvas);
            check(changed(before, hovered), "hover should reveal a visible legal route");
            canvas.stopEffects();
            passed++;
            System.out.println("PASS hover reveals path preview");
        });

        // Test 2: move animation is visual only, blocks another roll, then frees controls.
        CountDownLatch releaseDone = new CountDownLatch(1);
        AtomicReference<GameBoardView> boardRef = new AtomicReference<>();
        AtomicReference<BoardCanvas> canvasRef = new AtomicReference<>();
        AtomicReference<GameEngine> engineRef = new AtomicReference<>();
        edt(() -> {
            GameEngine engine = engine(6, 3);
            GameBoardView view = new GameBoardView(engine::elapsedSeconds, engine::rollDice, engine::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(view);
            canvas.setSize(640, 640);
            view.onStateChanged(engine.state());
            engine.addListener(view);
            engine.rollDice();
            check(engine.movePiece(0), "legal exit");
            check(canvas.isBusy(), "release animates");
            check(engine.state().piece(0).step() == 0, "Model position immediately correct");
            JButton roll = findRollButton(view);
            check(roll != null && !roll.isEnabled(), "roll disabled while moving");
            canvas.whenEffectsFinish(releaseDone::countDown);
            boardRef.set(view); canvasRef.set(canvas); engineRef.set(engine);
        });
        check(releaseDone.await(3, TimeUnit.SECONDS), "release animation must finish promptly");
        edt(() -> {
            check(!canvasRef.get().isBusy(), "movement finished");
            JButton roll = findRollButton(boardRef.get());
            check(roll != null && roll.isEnabled(), "roll restored after movement");
            engineRef.get().rollDice();
            check(engineRef.get().movePiece(0), "normal move started");
            check(canvasRef.get().isBusy(), "path movement animates");
            render(canvasRef.get());
            boardRef.get().stopClock();
            passed++;
            System.out.println("PASS release and step movement animate without modifying Model");
        });

        // Test 3: disable animation, no busy state and immediate gameplay.
        edt(() -> {
            GameEngine engine = engine(6);
            BoardCanvas canvas = new BoardCanvas(id -> {});
            canvas.setSize(640, 640);
            canvas.setState(engine.state());
            canvas.setAnimationsEnabled(false);
            engine.rollDice();
            Move release = MoveValidator.legalMoves(engine.state(), new DiceRoll(6)).get(0);
            canvas.setState(engine.state(), release);
            check(!canvas.isBusy(), "reduced-motion mode skips animations");
            render(canvas);
            canvas.stopEffects();
            passed++;
            System.out.println("PASS reduced-motion fallback");
        });

        // Test 4: final move waits until its finish celebration completes.
        CountDownLatch finishDone = new CountDownLatch(1);
        AtomicBoolean wasBusyAtGameOver = new AtomicBoolean(false);
        AtomicReference<GameBoardView> finalBoard = new AtomicReference<>();
        edt(() -> {
            GameEngine engine = engine(1);
            GameState state = engine.state();
            for (int i = 0; i < 3; i++) state.piece(i).moveTo(state.board().finishStep());
            state.piece(3).moveTo(state.board().finishStep() - 1);
            GameBoardView view = new GameBoardView(engine::elapsedSeconds, engine::rollDice, engine::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(view);
            canvas.setSize(640, 640);
            view.onStateChanged(state);
            engine.addListener(view);
            engine.addListener(new GameListener() {
                /** Record the game-over callback in the scripted scenario. */
                @Override public void onGameOver(GameResult result) {
                    wasBusyAtGameOver.set(canvas.isBusy());
                    view.whenVisualEffectsFinish(finishDone::countDown);
                }
            });
            engine.rollDice();
            check(engine.movePiece(3), "last piece finished");
            check(finishDone.getCount() == 1, "results deferred until finish celebration");
            finalBoard.set(view);
        });
        check(finishDone.await(4, TimeUnit.SECONDS), "finish and celebration should end");
        edt(() -> {
            check(wasBusyAtGameOver.get(), "game over fired during animation");
            finalBoard.get().stopClock();
            passed++;
            System.out.println("PASS finishing sparkle and deferred results");
        });
        // Test 5: leaving the screen cancels callbacks, preventing a stale results dialog.
        edt(() -> {
            GameEngine engine = engine(6);
            BoardCanvas canvas = new BoardCanvas(id -> {});
            canvas.setSize(640, 640);
            engine.rollDice();
            canvas.setState(engine.state(), MoveValidator.legalMoves(engine.state(), new DiceRoll(6)).get(0));
            AtomicBoolean staleCallback = new AtomicBoolean(false);
            canvas.whenEffectsFinish(() -> staleCallback.set(true));
            canvas.stopEffects();
            check(!staleCallback.get(), "cancelling the view must not run deferred actions");
            passed++;
            System.out.println("PASS leaving the board cancels deferred callbacks");
        });

        // Test 6: the animation option can be switched off safely during a movement.
        edt(() -> {
            GameEngine engine = engine(6);
            BoardCanvas canvas = new BoardCanvas(id -> {});
            canvas.setSize(640, 640);
            engine.rollDice();
            canvas.setState(engine.state(), MoveValidator.legalMoves(engine.state(), new DiceRoll(6)).get(0));
            AtomicBoolean completed = new AtomicBoolean(false);
            canvas.whenEffectsFinish(() -> completed.set(true));
            canvas.setAnimationsEnabled(false);
            check(!canvas.isBusy() && completed.get(), "disable should complete queued movement immediately");
            canvas.stopEffects();
            passed++;
            System.out.println("PASS animation toggle during movement");
        });
        System.out.printf("RESULT VisualEffectsSmokeTest: %d/%d passed%n", passed, 6);
    }

    /** Find the game board component inside the Swing window. */
    private static BoardCanvas findCanvas(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof BoardCanvas canvas) return canvas;
            if (child instanceof Container nested) {
                BoardCanvas found = findCanvas(nested);
                if (found != null) return found;
            }
        }
        return null;
    }
}
