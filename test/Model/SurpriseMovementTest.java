package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SurpriseMovementTest {

    private GameState state;
    private Piece piece;

    @BeforeEach
    void setUp() {
        GameEngine engine = new GameEngine(
                Difficulty.EASY,
                List.of("דור", "גל"),
                List.of(PlayerColor.RED, PlayerColor.GREEN));

        state = engine.state();
        piece = state.piece(0);
    }

    @Test
    void movesThreeStepsBackwardWithoutChangingState() {
        piece.moveTo(10);

        Move move = SurpriseMovement.backward(state, piece.id(), 3)
                .orElseThrow();

        assertAll(
                () -> assertEquals(piece.id(), move.pieceId()),
                () -> assertEquals(10, move.fromStep()),
                () -> assertEquals(7, move.toStep()),
                () -> assertFalse(move.partial()),
                () -> assertFalse(move.release()),
                () -> assertEquals(10, piece.step()));
    }

    @Test
    void stopsAtStartInsteadOfReturningToCastle() {
        piece.moveTo(1);

        Move move = SurpriseMovement.backward(state, piece.id(), 3)
                .orElseThrow();

        assertEquals(0, move.toStep());
        assertFalse(move.partial());
    }

    @Test
    void cannotMoveBackwardFromStart() {
        piece.moveTo(0);

        assertTrue(SurpriseMovement.backward(
                state, piece.id(), 3).isEmpty());
    }

    @Test
    void cannotMoveBackwardFromCastle() {
        assertTrue(SurpriseMovement.backward(
                state, piece.id(), 3).isEmpty());
    }

    @Test
    void cannotMoveBackwardFromFinish() {
        piece.moveTo(state.board().finishStep());

        assertTrue(SurpriseMovement.backward(
                state, piece.id(), 3).isEmpty());
    }

    @Test
    void stopsBeforeOpponentDoubleBlock() {
        piece.moveTo(10);
        placeOpponentBlockAtOwnStep(8);

        Move move = SurpriseMovement.backward(state, piece.id(), 3)
                .orElseThrow();

        assertEquals(9, move.toStep());
        assertTrue(move.partial());
    }

    @Test
    void cannotMoveWhenOpponentBlockIsImmediatelyBehind() {
        piece.moveTo(10);
        placeOpponentBlockAtOwnStep(9);

        assertTrue(SurpriseMovement.backward(
                state, piece.id(), 3).isEmpty());
    }

    @Test
    void rejectsNonPositiveStepCounts() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> SurpriseMovement.backward(
                                state, piece.id(), 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> SurpriseMovement.backward(
                                state, piece.id(), -1)));
    }

    @Test
    void directFinishBypassesOpponentBlockWithoutChangingState() {
        piece.moveTo(7);
        placeOpponentBlockAtOwnStep(8);

        Move move = SurpriseMovement.directFinish(
                state, piece.id()).orElseThrow();

        assertAll(
                () -> assertEquals(piece.id(), move.pieceId()),
                () -> assertEquals(7, move.fromStep()),
                () -> assertEquals(
                        state.board().finishStep(), move.toStep()),
                () -> assertFalse(move.partial()),
                () -> assertFalse(move.release()),
                () -> assertEquals(7, piece.step()));
    }

    @Test
    void returnToCastleCalculatesDestinationWithoutChangingState() {
        piece.moveTo(10);

        Move move = SurpriseMovement.returnToCastle(
                state, piece.id()).orElseThrow();

        assertAll(
                () -> assertEquals(piece.id(), move.pieceId()),
                () -> assertEquals(10, move.fromStep()),
                () -> assertEquals(Piece.IN_CASTLE, move.toStep()),
                () -> assertFalse(move.release()),
                () -> assertFalse(move.partial()),
                () -> assertEquals(10, piece.step()));
    }

    @Test
    void castlePieceCannotReceiveEitherDirectMove() {
        assertAll(
                () -> assertTrue(SurpriseMovement.directFinish(
                        state, piece.id()).isEmpty()),
                () -> assertTrue(SurpriseMovement.returnToCastle(
                        state, piece.id()).isEmpty()));
    }

    @Test
    void finishedPieceCannotReceiveEitherDirectMove() {
        piece.moveTo(state.board().finishStep());

        assertAll(
                () -> assertTrue(SurpriseMovement.directFinish(
                        state, piece.id()).isEmpty()),
                () -> assertTrue(SurpriseMovement.returnToCastle(
                        state, piece.id()).isEmpty()));
    }

    private void placeOpponentBlockAtOwnStep(int ownStep) {
        Position square = state.board().positionOf(
                piece.color(), ownStep);

        Player opponent = state.players().get(1);
        int opponentStep = state.board()
                .route(opponent.color()).indexOf(square);

        assertTrue(opponentStep >= 0,
                "The test square must be on the opponent's route");

        List<Piece> opponentPieces = state.piecesOf(opponent);
        opponentPieces.get(0).moveTo(opponentStep);
        opponentPieces.get(1).moveTo(opponentStep);
    }
}