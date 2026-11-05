package Controller;

import Model.*;
import View.*;

import java.util.List;
import java.util.Optional;

/**
 * Receives every user action, calls the Model and chooses which screen the View shows.
 * It holds no game rules and builds no dialogs itself (MVC, SRS 3.4.2).
 */
public final class GameController {
    private final MainWindow window = new MainWindow();
    private GameEngine engine;
    private GameBoardView boardView;
    private GameSetupView setupView;

    /** Show the application main menu. */
    public void start() { showMainMenu(); }

    /** Reset the current session and display the main menu. */
    private void showMainMenu() {
        leaveCurrentGame();
        window.showScreen(new MainMenuView(this::showSetup, this::showRules, this::exitApplication));
    }

    /** Show player and difficulty configuration. */
    private void showSetup() {
        setupView = new GameSetupView(this::startGame, this::showMainMenu);
        window.showScreen(setupView);
    }

    /** Display the Easy-level rule dialog. */
    private void showRules() { Dialogs.rules(window.component()); }

    /** FR-05: the game starts only with valid settings; otherwise the setup screen shows why. */
    private void startGame(Difficulty difficulty, List<String> names, List<PlayerColor> colors) {
        Optional<String> problem = SetupValidator.validate(difficulty, names, colors);
        if (problem.isPresent()) {
            setupView.showError(problem.get());
            return;
        }
        engine = new GameEngine(difficulty, names, colors);
        boardView = new GameBoardView(engine::elapsedSeconds, this::rollDice, this::selectPiece, this::confirmLeaveGame);
        engine.addListener(boardView);
        engine.addListener(new GameListener() {
            /** Notify the Controller that the game ended. */
            @Override public void onGameOver(GameResult result) { showResults(result); }
        });
        window.showScreen(boardView);
        boardView.onStateChanged(engine.state());
    }

    /** Ask the Model to roll while handling unexpected errors. */
    private void rollDice() { runSafely(() -> engine.rollDice()); }

    /** Pass the chosen pawn to the Model for validation. */
    private void selectPiece(int pieceId) { runSafely(() -> engine.movePiece(pieceId)); }

    /** Ask the player before discarding the current game. */
    private void confirmLeaveGame() {
        if (Dialogs.confirm(window.component(), "יציאה מהמשחק", "המשחק הנוכחי לא יישמר. לחזור למסך הפתיחה?")) {
            showMainMenu();
        }
    }

    /** FR-49: after the game ends only the results screen is shown. */
    private void showResults(GameResult result) {
        // Keep the final piece and finish celebration visible before changing screens.
        if (boardView != null) {
            boardView.whenVisualEffectsFinish(() -> showResultsNow(result));
        } else {
            showResultsNow(result);
        }
    }

    /** Stop animation timers and open the final ranking screen. */
    private void showResultsNow(GameResult result) {
        if (boardView != null) boardView.stopClock();
        Dialogs.info(window.component(), "סיום המשחק", "המנצח/ת: " + result.winner().name());
        leaveCurrentGame();
        window.showScreen(new ResultsView(result, this::showMainMenu));
    }

    /** Dispose game views and remove their observers. */
    private void leaveCurrentGame() {
        if (boardView != null) boardView.stopClock();
        if (engine != null && boardView != null) engine.removeListener(boardView);
        engine = null;
        boardView = null;
    }

    /** Close the application window and terminate the process. */
    private void exitApplication() {
        window.close();
        System.exit(0);
    }

    /** NFR-01: an unexpected error is reported to the user instead of closing the game. */
    private void runSafely(Runnable action) {
        if (engine == null) return;
        try {
            action.run();
        } catch (RuntimeException error) {
            error.printStackTrace();
            Dialogs.error(window.component(), "אירעה שגיאה לא צפויה: " + error.getMessage());
        }
    }
}
