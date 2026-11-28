package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Integration tests for FR-33–FR-37 with the original question movement rules. */
class QuestionMovementIntegrationTest {

    /** Load the JSON question bank without exposing answer indices to the View. */
    private static QuestionRepository repository() throws IOException {
        return QuestionRepository.fromResources();
    }

    /** Prepare one scripted move that lands on a question square. */
    private static GameEngine approachingQuestion(QuestionRepository repository,
            QuestionLevel level, int startStep, int roll) {
        GameEngine engine = TestSupport.newEngine(2, roll);
        Piece piece = engine.state().piece(0);
        piece.moveTo(startStep);
        Position landing = engine.state().board().positionOf(piece.color(), startStep + roll);
        engine.configureQuestions(new QuestionService(repository),
                square -> square.equals(landing) ? Optional.of(level) : Optional.empty());
        engine.rollDice();
        assertTrue(engine.movePiece(piece.id()));
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER, engine.state().phase());
        return engine;
    }

    /** Submit an actually correct or incorrect answer as determined by the JSON question bank. */
    private static QuestionService.AnswerResult answer(GameEngine engine,
            QuestionRepository repository, boolean shouldBeCorrect) {
        int questionId = engine.state().activeQuestion().orElseThrow().id();
        for (int choice = 0; choice < 4; choice++) {
            if (repository.isCorrect(questionId, choice) == shouldBeCorrect) {
                return engine.submitQuestionAnswer(choice).orElseThrow();
            }
        }
        throw new AssertionError("No matching choice");
    }

    /** Original Ludo Project-V2 p. 4: correct answers move 1, 2 or 3 squares forwards. */
    @Test void correctAnswerMovesAccordingToDifficulty() throws IOException {
        QuestionRepository repository = repository();
        for (QuestionLevel level : QuestionLevel.values()) {
            GameEngine engine = approachingQuestion(repository, level, 5, 2);
            assertTrue(answer(engine, repository, true).correct());
            assertEquals(7 + level.wrongAnswerPenalty(), engine.state().piece(0).step());
            assertTrue(engine.state().activeQuestion().isEmpty());
            assertTrue(engine.state().pendingQuestionResult().isEmpty());
            assertEquals(GameState.Phase.WAITING_FOR_ROLL, engine.state().phase());
            assertEquals(1, engine.state().currentPlayerIndex());
        }
    }

    /** Original Ludo Project-V2 p. 4: incorrect answers move 1, 2 or 3 squares backwards. */
    @Test void incorrectAnswerMovesAccordingToDifficulty() throws IOException {
        QuestionRepository repository = repository();
        for (QuestionLevel level : QuestionLevel.values()) {
            GameEngine engine = approachingQuestion(repository, level, 5, 2);
            assertFalse(answer(engine, repository, false).correct());
            assertEquals(7 - level.wrongAnswerPenalty(), engine.state().piece(0).step());
            assertTrue(engine.state().pendingQuestionResult().isEmpty());
        }
    }

    /** The piece cannot move into the castle when a wrong answer crosses the route start. */
    @Test void backwardPenaltyStopsAtTrackStart() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = approachingQuestion(repository, QuestionLevel.HARD, 0, 1);
        answer(engine, repository, false);
        assertEquals(0, engine.state().piece(0).step());
    }

    /** A correct question answer cannot bypass the exact-finish rule. */
    @Test void correctAnswerCannotOvershootFinish() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = TestSupport.newEngine(2, 1);
        Piece piece = engine.state().piece(0);
        int last = engine.state().board().finishStep() - 1;
        piece.moveTo(last - 1);
        Position landing = engine.state().board().positionOf(piece.color(), last);
        engine.configureQuestions(new QuestionService(repository),
                square -> square.equals(landing) ? Optional.of(QuestionLevel.HARD) : Optional.empty());
        engine.rollDice();
        assertTrue(engine.movePiece(piece.id()));
        answer(engine, repository, true);
        assertEquals(last, piece.step());
        assertTrue(engine.state().activeQuestion().isEmpty());
        assertEquals(1, engine.state().currentPlayerIndex());
    }

    /** An invalid choice does not consume the question and no second answer may be committed. */
    @Test void invalidAndDuplicateAnswerAreRejected() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = approachingQuestion(repository, QuestionLevel.EASY, 5, 2);
        assertTrue(engine.submitQuestionAnswer(4).isEmpty());
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER, engine.state().phase());
        int original = engine.state().piece(0).step();
        answer(engine, repository, true);
        assertEquals(original + 1, engine.state().piece(0).step());
        assertTrue(engine.submitQuestionAnswer(0).isEmpty());
        assertEquals(original + 1, engine.state().piece(0).step());
    }

    /** Observer emits an answer, a committed effect move, and an updated state in that order. */
    @Test void observerNotifiesAnswerAndMovementInOrder() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = approachingQuestion(repository, QuestionLevel.MEDIUM, 5, 2);
        List<String> events = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onQuestionAnswered(QuestionService.AnswerResult result, int pieceId) {
                events.add("ANSWER");
            }
            @Override public void onPieceMoved(Move move) { events.add("MOVE"); }
            @Override public void onStateChanged(GameState state) { events.add("STATE"); }
        });
        answer(engine, repository, true);
        assertEquals(List.of("ANSWER", "MOVE", "STATE"), events);
    }

    /** An enemy Double Block prevents both forwards and backwards passage. */
    @Test void questionMovementStopsBeforeOpponentDoubleBlock() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = TestSupport.newEngine(2, 2);
        GameState state = engine.state();
        Piece moving = state.piece(0);
        moving.moveTo(5);
        Position landing = state.board().positionOf(moving.color(), 7);
        Position blocked = state.board().positionOf(moving.color(), 8);
        Player opponent = state.players().get(1);
        int opponentStep = state.board().route(opponent.color()).indexOf(blocked);
        assertTrue(opponentStep >= 0, "Opponent route must include shared square");
        state.piecesOf(opponent).get(0).moveTo(opponentStep);
        state.piecesOf(opponent).get(1).moveTo(opponentStep);
        engine.configureQuestions(new QuestionService(repository),
                square -> square.equals(landing) ? Optional.of(QuestionLevel.HARD) : Optional.empty());
        engine.rollDice();
        assertTrue(engine.movePiece(0));
        answer(engine, repository, true);
        assertEquals(7, moving.step(), "Must not cross opponent's Double Block");
    }
    /** Question movement must capture an opponent when landing on an unprotected square. */
    @Test void questionEffectCanCaptureOpponent() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = TestSupport.newEngine(2, 2);
        GameState state = engine.state();
        Piece moving = state.piece(0);
        moving.moveTo(5);
        Player opponent = state.players().get(1);
        Position landing = state.board().positionOf(moving.color(), 7);
        Position captureSquare = state.board().positionOf(moving.color(), 10);
        int opponentStep = state.board().route(opponent.color()).indexOf(captureSquare);
        assertTrue(opponentStep >= 0);
        Piece victim = state.piecesOf(opponent).get(0);
        victim.moveTo(opponentStep);
        engine.configureQuestions(new QuestionService(repository),
                square -> square.equals(landing) ? Optional.of(QuestionLevel.HARD) : Optional.empty());
        engine.rollDice();
        assertTrue(engine.movePiece(0));
        answer(engine, repository, true);
        assertEquals(10, moving.step());
        assertTrue(victim.isInCastle(), "Victim must return to its castle");
    }

    /** A question effect landing on another question square must preserve the chained question. */
    @Test void movementCanTriggerAnotherQuestionWithoutPassingTurn() throws IOException {
        QuestionRepository repository = repository();
        GameEngine engine = TestSupport.newEngine(2, 2);
        Piece piece = engine.state().piece(0);
        piece.moveTo(5);
        Position first = engine.state().board().positionOf(piece.color(), 7);
        Position second = engine.state().board().positionOf(piece.color(), 8);
        engine.configureQuestions(new QuestionService(repository), square ->
                square.equals(first) ? Optional.of(QuestionLevel.EASY)
                : square.equals(second) ? Optional.of(QuestionLevel.MEDIUM) : Optional.empty());
        engine.rollDice();
        assertTrue(engine.movePiece(0));
        assertEquals(QuestionLevel.EASY, engine.state().activeQuestion().orElseThrow().level());
        answer(engine, repository, true);
        assertEquals(8, piece.step());
        assertEquals(QuestionLevel.MEDIUM, engine.state().activeQuestion().orElseThrow().level());
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER, engine.state().phase());
        assertEquals(0, engine.state().currentPlayerIndex());
        answer(engine, repository, true);
        assertEquals(10, piece.step());
        assertTrue(engine.state().activeQuestion().isEmpty());
        assertEquals(1, engine.state().currentPlayerIndex());
    }
}
