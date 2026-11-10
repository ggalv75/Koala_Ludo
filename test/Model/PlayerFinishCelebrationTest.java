package Model;

import View.BoardCanvas;
import View.GameBoardView;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

/** Regression and Swing checks for the celebration of a player finishing all four pieces. */
public final class PlayerFinishCelebrationTest {
    private static int passed;

    /** Fail immediately when a game or visual contract is broken. */
    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    /** Execute one checked scenario and report its success. */
    private static void test(String description, Runnable action) {
        action.run();
        passed++;
        System.out.println("PASS " + description);
    }

    /** Create a game with a scripted one-step roll and deterministic dependencies. */
    private static GameEngine engine(int players) {
        List<String> names = players == 2 ? List.of("Dana", "Omer") : List.of("Dana", "Omer", "Gal");
        List<PlayerColor> colors = players == 2 ? List.of(PlayerColor.RED, PlayerColor.GREEN)
                : List.of(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW);
        Dice fixed = new Dice(new Random(7)) {
            /** Return exactly one step for reproducible completion tests. */
            @Override public DiceRoll roll() { return new DiceRoll(1); }
        };
        return new GameEngine(Difficulty.EASY, names, colors, new Random(19), fixed, Clock.systemUTC());
    }

    /** Arrange a board with three completed pawns and the fourth one step from finishing. */
    private static void arrangeLastPiece(GameState state, int startId) {
        for (int i = startId; i < startId + 3; i++) state.piece(i).moveTo(state.board().finishStep());
        state.piece(startId + 3).moveTo(state.board().finishStep() - 1);
    }

    /** Find the real BoardCanvas inside the game screen without relying on a layout order. */
    private static BoardCanvas findCanvas(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof BoardCanvas canvas) return canvas;
            if (component instanceof Container child) {
                BoardCanvas found = findCanvas(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Read the rendered image using the same Java2D paint path as the real app. */
    private static BufferedImage frame(BoardCanvas canvas) {
        BufferedImage image = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        canvas.paint(g);
        g.dispose();
        return image;
    }

    /** Count changed pixels to prove the celebration paints a distinct overlay. */
    private static int differentPixels(BufferedImage first, BufferedImage second) {
        int different = 0;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) different++;
            }
        }
        return different;
    }

    /** Run the model event, Swing effect, reduced-motion and completion-order tests. */
    public static void main(String[] args) throws Exception {
        test("no player celebration after only the third pawn", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            s.piece(0).moveTo(s.board().finishStep());
            s.piece(1).moveTo(s.board().finishStep());
            s.piece(2).moveTo(s.board().finishStep() - 1);
            List<PlayerFinishEvent> seen = new ArrayList<>();
            e.addListener(new GameListener() {
                /** Record ranking events without changing any game rules. */
                @Override public void onPlayerFinished(PlayerFinishEvent event) { seen.add(event); }
            });
            e.rollDice();
            check(e.movePiece(2), "third pawn must move");
            check(seen.isEmpty(), "no finished-player event with just three pieces home");
            check(s.finishedPieceCount(s.players().get(0)) == 3, "exact count");
        });
        test("fourth pawn emits player finish before state without ending a three-player game", () -> {
            GameEngine e = engine(3);
            GameState s = e.state();
            arrangeLastPiece(s, 0);
            List<String> order = new ArrayList<>();
            List<PlayerFinishEvent> finishes = new ArrayList<>();
            e.addListener(new GameListener() {
                /** Record committed piece movement. */
                @Override public void onPieceMoved(Move m) { order.add("MOVE"); }
                /** Record the new player-rank announcement. */
                @Override public void onPlayerFinished(PlayerFinishEvent event) {
                    order.add("PLAYER_FINISH"); finishes.add(event);
                }
                /** Record state refresh to verify event sequencing. */
                @Override public void onStateChanged(GameState state) { order.add("STATE"); }
                /** Record a game-over callback if one occurs unexpectedly. */
                @Override public void onGameOver(GameResult result) { order.add("OVER"); }
            });
            e.rollDice(); order.clear();
            check(e.movePiece(3), "fourth pawn move accepted");
            check(order.equals(List.of("MOVE", "PLAYER_FINISH", "STATE")), "event order: " + order);
            check(finishes.size() == 1, "exactly one player-finish event");
            PlayerFinishEvent event = finishes.get(0);
            check(event.playerIndex() == 0 && event.color() == PlayerColor.RED && event.rank() == 1,
                    "correct winning player and rank");
            check("Dana".equals(event.playerName()), "name comes from model");
            check(s.phase() != GameState.Phase.GAME_OVER, "play continues for other players");
            check(s.currentPlayerIndex() == 1, "turn passed to next player");
        });
        test("fourth pawn emits completion before game-over and never celebrates unfinished last place", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            arrangeLastPiece(s, 0);
            List<String> order = new ArrayList<>();
            List<PlayerFinishEvent> events = new ArrayList<>();
            e.addListener(new GameListener() {
                /** Record movement before player completion. */
                @Override public void onPieceMoved(Move m) { order.add("MOVE"); }
                /** Record only genuinely completed players. */
                @Override public void onPlayerFinished(PlayerFinishEvent event) {
                    order.add("FINISH"); events.add(event);
                }
                /** Record state refresh before game-over. */
                @Override public void onStateChanged(GameState state) { order.add("STATE"); }
                /** Verify the final standings after the player completed their fourth pawn. */
                @Override public void onGameOver(GameResult result) { order.add("OVER"); }
            });
            e.rollDice(); order.clear();
            check(e.movePiece(3), "game-ending move accepted");
            check(order.equals(List.of("MOVE", "FINISH", "STATE", "OVER")), "order: " + order);
            check(events.size() == 1, "only the completing player is celebrated");
            check(s.result().standings().get(1).completedAllPieces() == false, "last place is unfinished");
        });
        test("player finish rank is accurate when another player already finished", () -> {
            GameEngine e = engine(3);
            GameState s = e.state();
            for (Piece piece : s.piecesOf(s.players().get(0))) piece.moveTo(s.board().finishStep());
            check(RankingManager.rankIfAllPiecesHome(s, s.players().get(0)), "first player ranked");
            s.setCurrentPlayerIndex(1);
            arrangeLastPiece(s, 4);
            List<PlayerFinishEvent> seen = new ArrayList<>();
            e.addListener(new GameListener() {
                /** Save the actual player-rank announcement. */
                @Override public void onPlayerFinished(PlayerFinishEvent event) { seen.add(event); }
            });
            e.rollDice(); check(e.movePiece(7), "second player's final move accepted");
            check(seen.size() == 1 && seen.get(0).rank() == 2, "second player gets second place");
            check(s.phase() == GameState.Phase.GAME_OVER, "one remaining player ends game");
        });
        test("invalid move cannot emit a player-finish event", () -> {
            GameEngine e = engine(2);
            arrangeLastPiece(e.state(), 0);
            List<PlayerFinishEvent> events = new ArrayList<>();
            e.addListener(new GameListener() {
                /** Capture ranking events for illegal action validation. */
                @Override public void onPlayerFinished(PlayerFinishEvent event) { events.add(event); }
            });
            e.rollDice();
            check(!e.movePiece(999), "illegal id rejected without exception");
            check(events.isEmpty(), "invalid action cannot celebrate a player");
            check(!e.state().currentPlayer().isFinished(), "rank remains unchanged");
        });
        System.setProperty("java.awt.headless", "true");
        AtomicReference<BoardCanvas> canvasRef = new AtomicReference<>();
        AtomicReference<GameBoardView> viewRef = new AtomicReference<>();
        AtomicReference<GameEngine> engineRef = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            GameEngine e = engine(3);
            GameState s = e.state();
            arrangeLastPiece(s, 0);
            GameBoardView view = new GameBoardView(e::elapsedSeconds, e::rollDice, e::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(view);
            check(canvas != null, "canvas present");
            canvas.setSize(640, 640);
            view.onStateChanged(s);
            e.addListener(view);
            e.rollDice(); check(e.movePiece(3), "fourth pawn moves on real view");
            check(canvas.isBusy(), "all effects are non-blocking and queued");
            canvas.whenEffectsFinish(done::countDown);
            canvasRef.set(canvas); viewRef.set(view); engineRef.set(e);
        });
        Thread.sleep(1050L); // The player celebration starts after the last pawn's sparkle.
        AtomicReference<BufferedImage> celebration = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            check(canvasRef.get().isBusy(), "player celebration is active after piece finish");
            celebration.set(frame(canvasRef.get()));
        });
        if (args.length > 0 && args[0].equals("preview")) {
            File preview = new File("docs/PlayerFinish_preview.png");
            preview.getParentFile().mkdirs();
            ImageIO.write(celebration.get(), "png", preview);
        }
        check(done.await(5, TimeUnit.SECONDS), "player celebration finishes and callbacks run");
        SwingUtilities.invokeAndWait(() -> {
            check(!canvasRef.get().isBusy(), "input unlocked only after full celebration");
            BufferedImage after = frame(canvasRef.get());
            check(differentPixels(celebration.get(), after) > 10_000, "visible celebration overlay drawn");
            viewRef.get().stopClock();
        });
        passed++;
        System.out.println("PASS visual celebration draws, finishes and releases input");
        SwingUtilities.invokeAndWait(() -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            arrangeLastPiece(s, 0);
            GameBoardView view = new GameBoardView(e::elapsedSeconds, e::rollDice, e::movePiece, () -> {});
            BoardCanvas canvas = findCanvas(view);
            check(canvas != null, "canvas exists with animations off");
            canvas.setSize(640, 640);
            canvas.setAnimationsEnabled(false);
            view.onStateChanged(s);
            e.addListener(view);
            e.rollDice();
            check(e.movePiece(3), "winning move still succeeds without effects");
            check(!canvas.isBusy(), "reduced-motion mode bypasses celebration");
            check(s.phase() == GameState.Phase.GAME_OVER, "game over unaffected");
            view.stopClock();
        });
        passed++;
        System.out.println("PASS reduced-motion mode bypasses celebration without changing the result");
        System.out.printf("RESULT PlayerFinishCelebrationTest: %d/%d passed%n", passed, passed);
    }
}
