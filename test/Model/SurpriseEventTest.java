package Model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SurpriseEventTest {

    @Test
    void preservesTheTriggeringPlayerPieceAndSurprise() {
        SurpriseEvent event = new SurpriseEvent(
                1, 4, SurpriseType.MOVE_OPPONENT_BACKWARD);

        assertAll(
                () -> assertEquals(1, event.playerIndex()),
                () -> assertEquals(4, event.pieceId()),
                () -> assertEquals(
                        SurpriseType.MOVE_OPPONENT_BACKWARD,
                        event.type()));
    }

    @Test
    void rejectsNegativePlayerIndex() {
        assertThrows(IllegalArgumentException.class,
                () -> new SurpriseEvent(
                        -1, 0, SurpriseType.DIRECT_FINISH));
    }

    @Test
    void rejectsNegativePieceId() {
        assertThrows(IllegalArgumentException.class,
                () -> new SurpriseEvent(
                        0, -1, SurpriseType.DIRECT_FINISH));
    }

    @Test
    void rejectsMissingSurpriseType() {
        assertThrows(NullPointerException.class,
                () -> new SurpriseEvent(0, 0, null));
    }
}