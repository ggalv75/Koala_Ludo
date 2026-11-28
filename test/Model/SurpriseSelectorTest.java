package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class SurpriseSelectorTest {

    @Test
    void everyTicketProducesTheRequiredOutcome() {
        for (int ticket = 0; ticket < 100; ticket++) {
            SurpriseType expected;

            if (ticket < 10) {
                expected = SurpriseType.DIRECT_FINISH;
            } else if (ticket < 20) {
                expected = SurpriseType.MOVE_OPPONENT_BACKWARD;
            } else if (ticket < 30) {
                expected = SurpriseType.RETURN_TO_CASTLE;
            } else if (ticket < 60) {
                expected = SurpriseType.MOVE_BACKWARD_THREE;
            } else if (ticket < 90) {
                expected = SurpriseType.LOSE_NEXT_TURN;
            } else {
                expected = SurpriseType.ULTRA_BOOST_ROLL;
            }

            SurpriseSelector selector =
                    new SurpriseSelector(new FixedRandom(ticket));

            assertEquals(expected, selector.draw(),
                    "Incorrect surprise for ticket " + ticket);
        }
    }

    @Test
    void allHundredTicketsProduceTheRequiredDistribution() {
        Map<SurpriseType, Integer> counts =
                new EnumMap<>(SurpriseType.class);

        for (int ticket = 0; ticket < 100; ticket++) {
            SurpriseSelector selector =
                    new SurpriseSelector(new FixedRandom(ticket));

            counts.merge(selector.draw(), 1, Integer::sum);
        }

        assertEquals(6, counts.size());
        assertEquals(10, counts.get(SurpriseType.DIRECT_FINISH).intValue());
        assertEquals(10, counts.get(
                SurpriseType.MOVE_OPPONENT_BACKWARD).intValue());
        assertEquals(10, counts.get(
                SurpriseType.RETURN_TO_CASTLE).intValue());
        assertEquals(30, counts.get(
                SurpriseType.MOVE_BACKWARD_THREE).intValue());
        assertEquals(30, counts.get(
                SurpriseType.LOSE_NEXT_TURN).intValue());
        assertEquals(10, counts.get(
                SurpriseType.ULTRA_BOOST_ROLL).intValue());
    }

    @Test
    void declaredProbabilitiesMatchTheRequirements() {
        assertEquals(10, SurpriseType.DIRECT_FINISH.probabilityPercent());
        assertEquals(10,
                SurpriseType.MOVE_OPPONENT_BACKWARD.probabilityPercent());
        assertEquals(10,
                SurpriseType.RETURN_TO_CASTLE.probabilityPercent());
        assertEquals(30,
                SurpriseType.MOVE_BACKWARD_THREE.probabilityPercent());
        assertEquals(30,
                SurpriseType.LOSE_NEXT_TURN.probabilityPercent());
        assertEquals(10,
                SurpriseType.ULTRA_BOOST_ROLL.probabilityPercent());
    }

    @Test
    void rejectsNullRandomSource() {
        assertThrows(NullPointerException.class,
                () -> new SurpriseSelector(null));
    }

    /** Supplies a chosen ticket instead of relying on chance. */
    private static final class FixedRandom extends Random {
        private static final long serialVersionUID = 1L;

        private final int ticket;

        FixedRandom(int ticket) {
            this.ticket = ticket;
        }

        @Override
        public int nextInt(int bound) {
            assertEquals(100, bound,
                    "The selector must draw from 100 equally likely tickets");
            return ticket;
        }
    }
}