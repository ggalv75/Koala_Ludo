package Controller;

import Model.*;
import View.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.SwingUtilities;

/**
 * Routes player actions and question Observer events between the Model and
 * Swing View.
 */
public final class GameController {
	private final MainWindow window = new MainWindow();
	private final GameSounds sounds;
	private GameEngine engine;
	private GameBoardView boardView;
	private GameSetupView setupView;

	/** Controller without audio (used by tests). */
	public GameController() {
		this(GameSounds.silent());
	}

	/** Controller that passes the given sound system to the screens. */
	public GameController(GameSounds sounds) {
		this.sounds = Objects.requireNonNull(sounds);
	}

	/** Show the application main menu. */
	public void start() {
		showMainMenu();
	}

	/** Reset the current session and display the main menu. */
	private void showMainMenu() {
		leaveCurrentGame();
		window.showScreen(new MainMenuView(this::showSetup, this::showRules, this::exitApplication, sounds));
	}

	/** Show player and difficulty configuration. */
	private void showSetup() {
		setupView = new GameSetupView(this::startGame, this::showMainMenu);
		window.showScreen(setupView);
	}

	/** Display the Easy-level rule dialog. */
	private void showRules() {
		Dialogs.rules(window.component());
	}

	/** Load the plain JSON question bank before starting a Medium game. */
	private boolean configureQuestionBank(GameEngine newEngine, Difficulty difficulty) {
		if (difficulty == Difficulty.EASY)
			return true;
		try {
			newEngine.configureQuestions(new QuestionService(QuestionRepository.fromResources()));
			return true;
		} catch (IOException | IllegalArgumentException error) {
			Dialogs.error(window.component(), "לא ניתן לטעון את מאגר השאלות (questions.json): " + error.getMessage());
			return false;
		}
	}

	/**
	 * FR-05: validate setup, start the engine and subscribe to game and question
	 * events.
	 */
	private void startGame(Difficulty difficulty, List<String> names, List<PlayerColor> colors) {
		Optional<String> problem = SetupValidator.validate(difficulty, names, colors);
		if (problem.isPresent()) {
			setupView.showError(problem.get());
			return;
		}
		GameEngine newEngine = new GameEngine(difficulty, names, colors);
		if (!configureQuestionBank(newEngine, difficulty))
			return;
		engine = newEngine;
		boardView = new GameBoardView(engine::elapsedSeconds, this::rollDice, this::selectPiece, this::confirmLeaveGame,
				sounds);
		engine.addListener(boardView);
		engine.addListener(new GameListener() {
			/** Schedule the question after the movement animation has completed. */
			@Override
			public void onQuestionRequested(Question question, int pieceId) {
				scheduleQuestion(question, pieceId);
			}

			/** Show the final result when the game ends. */
			@Override
			public void onGameOver(GameResult result) {
				showResults(result);
			}
		});
		window.showScreen(boardView);
		boardView.onStateChanged(engine.state());
	}

	/** Wait for the move to render before opening the mandatory question dialog. */
	private void scheduleQuestion(Question question, int pieceId) {
		GameEngine expectedEngine = engine;
		GameBoardView expectedView = boardView;
		SwingUtilities.invokeLater(() -> {
			if (expectedEngine == null || expectedView == null || engine != expectedEngine || boardView != expectedView)
				return;
			expectedView.whenVisualEffectsFinish(() -> SwingUtilities.invokeLater(() -> {
				if (engine != expectedEngine || boardView != expectedView)
					return;
				GameState state = expectedEngine.state();
				if (state.phase() != GameState.Phase.WAITING_FOR_QUESTION_ANSWER || state.activeQuestion().isEmpty()
						|| state.activeQuestion().get().id() != question.id()
						|| state.questionPieceId().orElse(-1) != pieceId)
					return;
				int selectedIndex = QuestionDialog.showQuestion(window.component(), question);

				runSafely(() -> expectedEngine.submitQuestionAnswer(selectedIndex)
						.ifPresent(result -> QuestionDialog.showAnswerFeedback(window.component(), result)));

			}));
		});
	}

	/** Ask the Model to roll while handling unexpected errors. */
	private void rollDice() {
		runSafely(() -> engine.rollDice());
	}

	/** Pass the chosen pawn to the Model for validation. */
	private void selectPiece(int pieceId) {
		runSafely(() -> engine.movePiece(pieceId));
	}

	/** Ask the player before discarding the current game. */
	private void confirmLeaveGame() {
		if (Dialogs.confirm(window.component(), "יציאה מהמשחק", "המשחק הנוכחי לא יישמר. לחזור למסך הפתיחה?")) {
			showMainMenu();
		}
	}

	/** FR-49: display rankings only after the final movement animation. */
	private void showResults(GameResult result) {
		if (boardView != null) {
			boardView.whenVisualEffectsFinish(() -> showResultsNow(result));
		} else {
			showResultsNow(result);
		}
	}

	/** Stop effects and open the final ranking screen. */
	private void showResultsNow(GameResult result) {
		if (boardView != null)
			boardView.stopClock();
		sounds.play(SoundEffect.GAME_OVER);
		Dialogs.info(window.component(), "סיום המשחק", "המנצח/ת: " + result.winner().name());
		leaveCurrentGame();
		window.showScreen(new ResultsView(result, this::showMainMenu));
	}

	/** Dispose game views and remove their observers. */
	private void leaveCurrentGame() {
		if (boardView != null)
			boardView.stopClock();
		if (engine != null && boardView != null)
			engine.removeListener(boardView);
		engine = null;
		boardView = null;
	}

	/** Close the application window and terminate the process. */
	private void exitApplication() {
		sounds.shutdown();
		window.close();
		System.exit(0);
	}

	/** Report unexpected errors without crashing the application (NFR-01). */
	private void runSafely(Runnable action) {
		if (engine == null)
			return;
		try {
			action.run();
		} catch (RuntimeException error) {
			error.printStackTrace();
			Dialogs.error(window.component(), "אירעה שגיאה לא צפויה: " + error.getMessage());
		}
	}
}
