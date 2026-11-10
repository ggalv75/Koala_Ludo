package Model;

import View.GameBoardView;
import View.DieFace;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.util.List;
import java.util.Random;

/** Verify the previous die result remains visible until another real roll is made. */
public final class PersistentDiceResultTest {
    private static int passed;

    /** Fail clearly if an expected game or screen condition is not satisfied. */
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Build a predictable die for scenarios that include automatic turn changes. */
    private static Dice scriptedDice(int... values) {
        return new Dice(new Random(0)) {
            private int index;

            /** Return the next predetermined die face for a regression scenario. */
            @Override public DiceRoll roll() {
                if (index >= values.length) throw new AssertionError("Scripted die exhausted");
                return new DiceRoll(values[index++]);
            }
        };
    }

    /** Start an Easy game whose roll results are deterministic. */
    private static GameEngine game(int... values) {
        return new GameEngine(Difficulty.EASY, List.of("Koala", "Panda"),
                List.of(PlayerColor.RED, PlayerColor.BLUE),
                new Random(42), scriptedDice(values), Clock.systemUTC());
    }

    /** Create a game screen that receives all actual Engine state updates. */
    private static GameBoardView screen(GameEngine engine) {
        GameBoardView view = new GameBoardView(engine::elapsedSeconds,
                engine::rollDice, pieceId -> engine.movePiece(pieceId), () -> { });
        engine.addListener(view);
        view.onStateChanged(engine.state());
        return view;
    }

    /** Find the actual animated die component within the game screen. */
    private static DieFace findDie(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof DieFace die) return die;
            if (component instanceof Container nested) {
                DieFace found = findDie(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Render the same actual die widget used during gameplay for visual comparison. */
    private static BufferedImage renderDie(DieFace die) {
        die.setSize(86, 86);
        BufferedImage image = new BufferedImage(86, 86, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            die.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /** Confirm the physical die face shows the expected pips, not just the label. */
    private static void expectDieFace(GameBoardView view, int expected) {
        DieFace actual = findDie(view);
        check(actual != null, "Die widget must be present");
        DieFace reference = new DieFace();
        reference.setValue(expected);
        BufferedImage displayed = renderDie(actual);
        BufferedImage desired = renderDie(reference);
        for (int y = 0; y < 86; y++) {
            for (int x = 0; x < 86; x++) {
                check(displayed.getRGB(x, y) == desired.getRGB(x, y),
                        "Die must visually show " + expected + " after a turn change");
            }
        }
    }

    /** Locate the die-result text without exposing private Swing widgets. */
    private static String resultLabel(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel label && (label.getText().contains("הטלה")
                    || label.getText().contains("ממתין להטלה"))) {
                return label.getText();
            }
            if (component instanceof Container nested) {
                String found = resultLabel(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Confirm a new game has no invented previous die result. */
    private static void initialState() {
        GameEngine engine = game(2);
        GameBoardView view = screen(engine);
        try {
            check(engine.state().latestRoll() == null, "A new game must not show a fake result");
            check("ממתין להטלה".equals(resultLabel(view)), "New game should wait for first roll");
            expectDieFace(view, 0);
        } finally {
            view.stopClock();
        }
    }

    /** Keep the rolled value when an impossible move immediately passes the turn. */
    private static void noLegalMovePassesTurn() {
        GameEngine engine = game(2);
        GameBoardView view = screen(engine);
        try {
            engine.rollDice();
            check(engine.state().currentPlayerIndex() == 1, "No-move roll must pass to the next player");
            check(engine.state().lastRoll() == null, "Active turn's roll must still be cleared");
            check(engine.state().latestRoll().value() == 2, "Previous real result must be remembered");
            check("ההטלה האחרונה: 2".equals(resultLabel(view)), "Die result must stay visible");
            expectDieFace(view, 2);
        } finally {
            view.stopClock();
        }
    }

    /** Replace the previous face only after the next actual Model roll. */
    private static void nextRollReplacesPrevious() {
        GameEngine engine = game(2, 3);
        GameBoardView view = screen(engine);
        try {
            engine.rollDice();
            check(engine.state().latestRoll().value() == 2, "First face must be saved");
            engine.rollDice();
            check(engine.state().latestRoll().value() == 3, "New roll must replace previous face");
            check("ההטלה האחרונה: 3".equals(resultLabel(view)), "New roll must appear on screen");
            expectDieFace(view, 3);
        } finally {
            view.stopClock();
        }
    }

    /** Keep a result visible after a completed move passes control onward. */
    private static void movementThenTurnChange() {
        GameEngine engine = game(6, 1);
        GameBoardView view = screen(engine);
        try {
            engine.rollDice();
            check("תוצאת ההטלה: 6".equals(resultLabel(view)), "Current six should be visible");
            check(engine.movePiece(engine.state().legalMoves().get(0).pieceId()), "Castle release should work");
            engine.rollDice();
            check(engine.movePiece(engine.state().legalMoves().get(0).pieceId()), "Step move should work");
            check(engine.state().currentPlayerIndex() == 1, "A normal move should pass the turn");
            check(engine.state().lastRoll() == null, "Turn-scoped roll must be reset");
            check(engine.state().latestRoll().value() == 1, "Last roll must survive turn change");
            check("ההטלה האחרונה: 1".equals(resultLabel(view)), "Result must remain for next player");
            expectDieFace(view, 1);
        } finally {
            view.stopClock();
        }
    }

    /** Preserve even a cancelled third six as the latest physical die face. */
    private static void thirdSixStillVisible() {
        GameEngine engine = game(6, 6, 6);
        GameBoardView view = screen(engine);
        try {
            for (int i = 0; i < 2; i++) {
                engine.rollDice();
                check(engine.movePiece(engine.state().legalMoves().get(0).pieceId()),
                        "A legal six move is expected before the next roll");
            }
            engine.rollDice();
            check(engine.state().currentPlayerIndex() == 1, "Third consecutive six cancels the turn");
            check(engine.state().lastRoll() == null, "Cancelled roll must not remain active");
            check(engine.state().latestRoll().value() == 6, "Cancelled die face is still the last shown face");
            check("ההטלה האחרונה: 6".equals(resultLabel(view)), "Last six should remain visible");
            expectDieFace(view, 6);
        } finally {
            view.stopClock();
        }
    }

    /** Run the visual state regression scenarios on Swing's event thread. */
    public static void main(String[] args) throws Exception {
        Runnable[] scenarios = {
                PersistentDiceResultTest::initialState,
                PersistentDiceResultTest::noLegalMovePassesTurn,
                PersistentDiceResultTest::nextRollReplacesPrevious,
                PersistentDiceResultTest::movementThenTurnChange,
                PersistentDiceResultTest::thirdSixStillVisible
        };
        for (Runnable scenario : scenarios) {
            SwingUtilities.invokeAndWait(scenario);
            passed++;
        }
        System.out.println("PASS: " + passed + " persistent die result and turn-change checks");
    }
}
