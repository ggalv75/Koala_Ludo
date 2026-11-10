package View;

import Model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Headless regression checks for the visual die roll and the game screen's input gate. */
public final class DieRollAnimationTest {
    /** Assert a condition without requiring Java's optional -ea flag. */
    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Execute one short test on the Swing event thread. */
    private static void onEdt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }

    /** Locate a child Swing component by its class. */
    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) return type.cast(component);
            if (component instanceof Container nested) {
                T match = find(nested, type);
                if (match != null) return match;
            }
        }
        return null;
    }

    /** Draw the die offscreen to ensure each side renders without throwing. */
    private static void testRendering() throws Exception {
        onEdt(() -> {
            DieFace die = new DieFace();
            die.setSize(86, 86);
            for (int side = 0; side <= 6; side++) {
                die.setValue(side);
                BufferedImage frame = new BufferedImage(86, 86, BufferedImage.TYPE_INT_ARGB);
                die.paint(frame.createGraphics());
                expect(die.value() == side, "side display mismatch");
                expect((frame.getRGB(43, 43) >>> 24) > 0, "die should have visible center");
            }
        });
    }

    /** Confirm a double-start cannot generate two rolls. */
    private static void testOneRealRoll() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<DieFace> ref = new AtomicReference<>();
        onEdt(() -> {
            DieFace die = new DieFace();
            die.setSize(86, 86);
            die.setValue(2);
            ref.set(die);
            die.animateRoll(() -> { calls.incrementAndGet(); die.setValue(5); done.countDown(); });
            die.animateRoll(() -> calls.addAndGet(100));
            expect(die.isRolling(), "die should begin animating");
            expect(die.value() == 2, "animation must not override actual Model result");
        });
        expect(done.await(3, TimeUnit.SECONDS), "visual roll should complete in time");
        onEdt(() -> {
            expect(!ref.get().isRolling(), "die should be idle after callback");
            expect(ref.get().value() == 5, "die should finish on authoritative result");
            ref.get().finishRollAnimation();
            expect(calls.get() == 1, "Model action must run exactly once");
        });
    }

    /** Confirm canceling an unfinished animation never rolls the Model. */
    private static void testCancellation() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<DieFace> ref = new AtomicReference<>();
        onEdt(() -> {
            DieFace die = new DieFace();
            die.setValue(3);
            ref.set(die);
            die.animateRoll(calls::incrementAndGet);
            die.cancelRollAnimation();
            expect(!die.isRolling(), "cancel should stop animation");
            expect(die.value() == 3, "cancel should preserve last Model result");
        });
        Thread.sleep(950);
        onEdt(() -> expect(calls.get() == 0, "cancel should never trigger a late roll"));
    }

    /** Confirm skipping animation triggers its pending roll once. */
    private static void testFinishNow() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        onEdt(() -> {
            DieFace die = new DieFace();
            die.animateRoll(calls::incrementAndGet);
            die.finishRollAnimation();
            die.finishRollAnimation();
            expect(!die.isRolling() && calls.get() == 1, "finish-now should roll exactly once");
        });
    }

    /** Verify the screen blocks repeated clicks until one real Model roll is complete. */
    private static void testGameScreenIntegration() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<GameBoardView> viewRef = new AtomicReference<>();
        onEdt(() -> {
            GameEngine engine = new GameEngine(Difficulty.EASY,
                    List.of("Koala", "Panda"), List.of(PlayerColor.BLUE, PlayerColor.GREEN));
            GameBoardView view = new GameBoardView(engine::elapsedSeconds, () -> {
                calls.incrementAndGet();
                engine.rollDice();
                done.countDown();
            }, piece -> { }, () -> { });
            engine.addListener(view);
            view.onStateChanged(engine.state());
            viewRef.set(view);
            JButton button = find(view, JButton.class);
            expect(button != null, "roll button missing");
            // The first JButton in the screen may be the exit button.
            button = findButton(view, "הטלת קובייה");
            expect(button != null && button.isEnabled(), "roll button should start enabled");
            button.doClick();
            expect(!button.isEnabled(), "roll button should lock during animation");
            expect(calls.get() == 0, "Model must not roll before animation ends");
            button.doClick();
            expect(calls.get() == 0, "repeated click must not bypass animation");
        });
        expect(done.await(3, TimeUnit.SECONDS), "roll action should run after animation");
        onEdt(() -> {
            expect(calls.get() == 1, "game should receive precisely one roll");
            viewRef.get().stopClock();
        });
    }

    /** Locate the visible button by its Hebrew caption. */
    private static JButton findButton(Container root, String caption) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && caption.equals(button.getText())) return button;
            if (component instanceof Container nested) {
                JButton found = findButton(nested, caption);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Verify disabling visual effects commits the next roll immediately. */
    private static void testDisabledAnimation() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        onEdt(() -> {
            GameEngine engine = new GameEngine(Difficulty.EASY,
                    List.of("Koala", "Panda"), List.of(PlayerColor.RED, PlayerColor.BLUE));
            GameBoardView view = new GameBoardView(engine::elapsedSeconds,
                    () -> { calls.incrementAndGet(); engine.rollDice(); }, piece -> { }, () -> { });
            engine.addListener(view);
            view.onStateChanged(engine.state());
            JCheckBox toggle = find(view, JCheckBox.class);
            expect(toggle != null && toggle.isSelected(), "animation toggle should start on");
            toggle.doClick();
            JButton button = findButton(view, "הטלת קובייה");
            expect(button != null && button.isEnabled(), "roll should be enabled");
            button.doClick();
            expect(calls.get() == 1, "animation-off should roll immediately");
            view.stopClock();
        });
    }

    /** Run the complete die animation suite. */
    public static void main(String[] args) throws Exception {
        testRendering();
        testOneRealRoll();
        testCancellation();
        testFinishNow();
        testGameScreenIntegration();
        testDisabledAnimation();
        System.out.println("PASS: 6 dice animation + gameplay integration checks");
    }
}
