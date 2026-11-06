package Model;

import java.util.*;

/** Finishing order and end of game: FR-45, FR-46, FR-47. */
public final class RankingManager {
    /** Prevent instantiation of the ranking rules helper. */
    private RankingManager() { }

    /** FR-45: when all four pieces are home the player gets the next free place. Returns true if ranked now. */
    static boolean rankIfCompleted(GameState state, Player player) {
        if (player.isFinished() || state.finishedPieceCount(player) < GameRules.PIECES_PER_PLAYER) return false;
        player.assignRank(nextRank(state), true);
        return true;
    }

    /** FR-47: the game ends when only one player has not finished. */
    static boolean isGameOver(GameState state) {
        return state.players().stream().filter(player -> !player.isFinished()).count() <= 1;
    }

    /** FR-47: the remaining player takes the last place without completing. Returns players by rank. */
    static List<Player> finalStandings(GameState state) {
        for (Player player : state.players()) {
            if (!player.isFinished()) player.assignRank(nextRank(state), false);
        }
        List<Player> standings = new ArrayList<>(state.players());
        standings.sort(Comparator.comparingInt(Player::rank));
        return standings;
    }

    /** Calculate the next available finishing position. */
    private static int nextRank(GameState state) {
        return 1 + (int) state.players().stream().filter(Player::isFinished).count();
    }
}
