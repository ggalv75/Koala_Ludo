package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-45, FR-46, FR-47: finishing order and end of game in RankingManager. Owner: Gal (SDP 5.2.1). */
class RankingManagerTest {

    @Test
    @DisplayName("FR-45 boundary: three pieces home is not enough to finish")
    void threePiecesHomeIsNotFinished() {
        GameState state = TestSupport.newState(2);
        Player gal = state.players().get(0);
        List<Piece> pieces = state.piecesOf(gal);
        for (int i = 0; i < 3; i++) pieces.get(i).moveTo(state.board().finishStep());

        assertFalse(RankingManager.rankIfAllPiecesHome(state, gal));
        assertEquals(0, gal.rank());
        assertFalse(gal.isFinished());
    }

    @Test
    @DisplayName("FR-45: the first player to bring all four pieces home gets first place")
    void firstToFinishGetsFirstPlace() {
        GameState state = TestSupport.newState(4);
        Player gal = state.players().get(0);
        TestSupport.bringAllPiecesHome(state, gal);

        assertTrue(RankingManager.rankIfAllPiecesHome(state, gal));
        assertEquals(1, gal.rank());
        assertTrue(gal.completedAllPieces());
    }

    @Test
    @DisplayName("FR-45: a player who is already ranked is not ranked again")
    void rankedPlayerKeepsTheirRank() {
        GameState state = TestSupport.newState(4);
        Player gal = state.players().get(0);
        TestSupport.bringAllPiecesHome(state, gal);
        RankingManager.rankIfAllPiecesHome(state, gal);

        assertFalse(RankingManager.rankIfAllPiecesHome(state, gal));
        assertEquals(1, gal.rank());
    }

    @Test
    @DisplayName("FR-45: places follow the finishing order, not the turn order")
    void placesFollowFinishingOrder() {
        GameState state = TestSupport.newState(4);
        Player p1 = state.players().get(0);
        Player p2 = state.players().get(1);
        Player p3 = state.players().get(2);

        finish(state, p3);
        finish(state, p1);
        finish(state, p2);

        assertEquals(1, p3.rank());
        assertEquals(2, p1.rank());
        assertEquals(3, p2.rank());
    }

    @Test
    @DisplayName("FR-47 boundary, 2 players: the game ends as soon as one player finishes")
    void twoPlayersGameEndsAfterFirstFinisher() {
        GameState state = TestSupport.newState(2);
        assertFalse(RankingManager.isGameOver(state));
        finish(state, state.players().get(0));
        assertTrue(RankingManager.isGameOver(state));
    }

    @Test
    @DisplayName("FR-47, 3 players: the game goes on after the first finisher and ends after the second")
    void threePlayersGameEndsAfterSecondFinisher() {
        GameState state = TestSupport.newState(3);
        finish(state, state.players().get(0));
        assertFalse(RankingManager.isGameOver(state));
        finish(state, state.players().get(1));
        assertTrue(RankingManager.isGameOver(state));
    }

    @Test
    @DisplayName("FR-47 boundary, 4 players: the game ends when three players have finished")
    void fourPlayersGameEndsAfterThirdFinisher() {
        GameState state = TestSupport.newState(4);
        finish(state, state.players().get(0));
        finish(state, state.players().get(1));
        assertFalse(RankingManager.isGameOver(state));
        finish(state, state.players().get(2));
        assertTrue(RankingManager.isGameOver(state));
    }

    @Test
    @DisplayName("FR-47: the player left in the game takes the last place, as a loss")
    void remainingPlayerTakesLastPlace() {
        GameState state = TestSupport.newState(3);
        Player last = state.players().get(2);
        finish(state, state.players().get(1));
        finish(state, state.players().get(0));

        RankingManager.rankRemainingPlayers(state);

        assertEquals(3, last.rank());
        assertFalse(last.completedAllPieces());
    }

    @Test
    @DisplayName("FR-47: the final standings are sorted from first place to last")
    void standingsAreSortedByPlace() {
        GameState state = TestSupport.newState(3);
        finish(state, state.players().get(2));
        finish(state, state.players().get(0));
        RankingManager.rankRemainingPlayers(state);

        List<GameResult.Standing> standings = RankingManager.standings(state);

        assertEquals(List.of(1, 2, 3), standings.stream().map(GameResult.Standing::rank).toList());
        assertEquals("P3", standings.get(0).name());
        assertEquals("P2", standings.get(2).name());
    }

    @Test
    @DisplayName("FR-45, FR-47: in a 4-player game the places are 1-3 by finishing order and 4 for the last")
    void fourPlayerFullRanking() {
        GameState state = TestSupport.newState(4);
        finish(state, state.players().get(2));
        finish(state, state.players().get(0));
        finish(state, state.players().get(3));
        RankingManager.rankRemainingPlayers(state);

        assertEquals(List.of(1, 2, 3, 4), RankingManager.standings(state).stream()
                .map(GameResult.Standing::rank).toList());
        assertEquals(List.of("P3", "P1", "P4", "P2"), RankingManager.standings(state).stream()
                .map(GameResult.Standing::name).toList());
        assertFalse(state.players().get(1).completedAllPieces());
    }


    private static void finish(GameState state, Player player) {
        TestSupport.bringAllPiecesHome(state, player);
        RankingManager.rankIfAllPiecesHome(state, player);
    }
}
