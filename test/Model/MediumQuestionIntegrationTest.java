package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** End-to-end Model integration of Medium question square, Observer and FR-37 movement. */
class MediumQuestionIntegrationTest {

    /** Always roll three steps with Boost multiplier two (factor one). */
    private static Dice scriptedMediumDice() {
        return new Dice() {
            /** A fixed regular die avoids flaky random tests. */
            @Override public DiceRoll roll() { return new DiceRoll(3); }
            /** A fixed Boost multiplier gives three route steps. */
            @Override public int rollMultiplier(MultiplierDie die) {
                assertEquals(MultiplierDie.BOOST, die);
                return 2;
            }
        };
    }

    /** Find a question square and arrange a legitimate Medium roll that lands on it. */
    private static GameEngine mediumGameLandingOnQuestion(QuestionRepository repository) {
        GameEngine engine = new GameEngine(Difficulty.MEDIUM,
                List.of("Red", "Green"), List.of(PlayerColor.RED, PlayerColor.GREEN),
                new Random(42), scriptedMediumDice(), Clock.systemUTC());
        GameState state = engine.state();
        Piece moving = state.piecesOf(state.currentPlayer()).get(0);
        List<Position> route = state.board().route(moving.color());
        int landing = -1;
        for (int step = 3; step < state.board().homePathStart(); step++) {
            CellType type = state.board().cellAt(route.get(step)).type();
            if (type == CellType.QUESTION_EASY || type == CellType.QUESTION_MEDIUM
                    || type == CellType.QUESTION_HARD) {
                landing = step;
                break;
            }
        }
        assertTrue(landing >= 3, "Expected at least one question square on the active route");
        moving.moveTo(landing - 3);
        engine.configureQuestions(new QuestionService(repository));
        engine.rollDice();
        assertTrue(engine.movePiece(moving.id()), "The die move must land on the question square");
        assertEquals(landing, moving.step());
        return engine;
    }

    /** Real board symbols must trigger a question, and an answer must move the same pawn. */
    @Test void questionOnMediumBoardOpensAndMovesAfterAnswer() throws IOException {
        QuestionRepository repository = QuestionRepository.fromResources(new Random(19));
        GameEngine engine = mediumGameLandingOnQuestion(repository);
        GameState state = engine.state();
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER, state.phase());
        Question q = state.activeQuestion().orElseThrow();
        assertEquals(4, q.options().size());
        int oldStep = state.piece(0).step();
        int correct = -1;
        for (int index = 0; index < 4; index++) {
            if (repository.isCorrect(q.id(), index)) correct = index;
        }
        assertTrue(correct >= 0);
        assertTrue(engine.submitQuestionAnswer(correct).orElseThrow().correct());
        assertEquals(oldStep + q.level().wrongAnswerPenalty(), state.piece(0).step());
        assertTrue(state.pendingQuestionResult().isEmpty());
    }
}
