package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Integration tests for the complete Observer question flow (FR-33 to FR-37). */
class QuestionObserverIntegrationTest {

    /** Load the 30-question resource from the bundled JSON resource. */
    private static QuestionRepository repository() throws IOException {
        return QuestionRepository.fromResources();
    }

    /** Request an Easy question from a deterministic landing on the first start square. */
    private static GameEngine engineWithQuestion(QuestionRepository repository) {
        GameEngine engine = TestSupport.newEngine(2, 6);
        Position firstLanding = engine.state().board().positionOf(engine.state().piece(0).color(), 0);
        engine.configureQuestions(new QuestionService(repository),
                square -> square.equals(firstLanding) ?
                        Optional.of(QuestionLevel.EASY) : Optional.empty());
        return engine;
    }

    /** Verify movement, question, then state events and paused turn. */
    @Test
    void landingPublishesQuestionAndPausesTurn() throws IOException {
        GameEngine engine = engineWithQuestion(repository());
        List<String> events = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onPieceMoved(Move move) { events.add("MOVE"); }
            @Override public void onQuestionRequested(Question q, int id) { events.add("QUESTION"); }
            @Override public void onStateChanged(GameState state) { events.add("STATE"); }
        });

        engine.rollDice();
        events.clear();
        assertTrue(engine.movePiece(0));

        assertEquals(List.of("MOVE", "QUESTION", "STATE"), events);
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER, engine.state().phase());
        assertTrue(engine.state().activeQuestion().isPresent());
        assertEquals(QuestionLevel.EASY, engine.state().activeQuestion().orElseThrow().level());
        assertEquals(0, engine.state().questionPieceId().orElseThrow());
        assertTrue(engine.state().legalMoves().isEmpty());
        assertEquals(0, engine.state().currentPlayerIndex());

        engine.rollDice();
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER, engine.state().phase());
        assertFalse(engine.movePiece(0));
    }

    /** Verify that an answer triggers its movement effect and completes the question. */
    @Test
    void answerIsPublishedOnceAndMovementIsApplied() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = engineWithQuestion(repository);
        List<String> events = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onQuestionAnswered(QuestionService.AnswerResult r, int id) {
                events.add("ANSWER:" + id);
            }
            @Override public void onPieceMoved(Move move) { events.add("MOVE"); }
            @Override public void onStateChanged(GameState state) { events.add("STATE"); }
        });
        engine.rollDice();
        assertTrue(engine.movePiece(0));
        int questionId = engine.state().activeQuestion().orElseThrow().id();
        events.clear();
        assertTrue(engine.submitQuestionAnswer(-1).isEmpty());
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER, engine.state().phase());
        events.clear();

        QuestionService.AnswerResult result = engine.submitQuestionAnswer(0).orElseThrow();
        assertEquals(repository.isCorrect(questionId, 0), result.correct());
        assertTrue(engine.state().activeQuestion().isEmpty());
        assertTrue(engine.state().pendingQuestionResult().isEmpty());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, engine.state().phase());
        assertEquals(result.correct() ? 1 : 0, engine.state().piece(0).step());
        assertEquals(result.correct() ? List.of("ANSWER:0", "MOVE", "STATE")
                : List.of("ANSWER:0", "STATE"), events);
        assertTrue(engine.submitQuestionAnswer(1).isEmpty(), "No second answer is accepted");
    }

    /** An Easy game with no question service must preserve its existing roll-after-six behavior. */
    @Test
    void easyGameWithoutQuestionsRetainsExistingTurnFlow() {
        GameEngine engine = TestSupport.newEngine(2, 6);
        engine.rollDice();
        assertTrue(engine.movePiece(0));
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, engine.state().phase());
        assertTrue(engine.state().activeQuestion().isEmpty());
        assertTrue(engine.state().questionPieceId().isEmpty());
    }

    /** A configured service never triggers a question on an ordinary square. */
    @Test
    void ordinarySquareDoesNotTriggerAQuestion() throws IOException {
        GameEngine engine = TestSupport.newEngine(2, 6);
        engine.configureQuestions(new QuestionService(repository()), square -> Optional.empty());
        engine.rollDice();
        assertTrue(engine.movePiece(0));
        assertEquals(GameState.Phase.WAITING_FOR_ROLL, engine.state().phase());
    }

    /** Removed observers must stop receiving notifications. */
    @Test
    void removedObserverDoesNotReceiveQuestionEvents() throws IOException {
        GameEngine engine = engineWithQuestion(repository());
        List<String> received = new ArrayList<>();
        GameListener removed = new GameListener() {
            @Override public void onQuestionRequested(Question question, int pieceId) {
                received.add("removed");
            }
        };
        engine.addListener(removed);
        engine.removeListener(removed);
        engine.rollDice();
        assertTrue(engine.movePiece(0));
        assertTrue(received.isEmpty());
    }

}
