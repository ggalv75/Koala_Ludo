package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Integration tests for the staged Observer question flow (FR-33 to FR-36). */
class QuestionObserverIntegrationTest {

    /** Load the 30-question resource with the local development key from stage 1. */
    private static QuestionRepository repository() throws IOException {
        return QuestionRepository.fromResources(
                QuestionRepository.readKey(Path.of("local-only/questions.key")));
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

    /** Verify answer feedback, no answer-key leak and movement not yet applied. */
    @Test
    void answerIsPublishedOnceButEffectIsDeferred() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = engineWithQuestion(repository);
        List<String> events = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onQuestionAnswered(QuestionService.AnswerResult r, int id) {
                events.add("ANSWER:" + id);
            }
            @Override public void onStateChanged(GameState state) { events.add("STATE"); }
        });
        engine.rollDice();
        assertTrue(engine.movePiece(0));
        int questionId = engine.state().activeQuestion().orElseThrow().id();
        int originalStep = engine.state().piece(0).step();
        events.clear();

        assertTrue(engine.submitQuestionAnswer(-1).isEmpty());
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER, engine.state().phase());
        assertTrue(engine.state().activeQuestion().isPresent());
        events.clear();

        QuestionService.AnswerResult result = engine.submitQuestionAnswer(0).orElseThrow();
        assertEquals(List.of("ANSWER:0", "STATE"), events);
        assertEquals(repository.isCorrect(questionId, 0), result.correct());
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_EFFECT, engine.state().phase());
        assertTrue(engine.state().activeQuestion().isEmpty());
        assertEquals(result, engine.state().pendingQuestionResult().orElseThrow());
        assertEquals(originalStep, engine.state().piece(0).step(), "FR-37 is handled in a later stage");
        assertEquals(0, engine.state().currentPlayerIndex());
        assertTrue(engine.submitQuestionAnswer(1).isEmpty(), "No second answer is accepted");
        engine.rollDice();
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_EFFECT, engine.state().phase());
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
