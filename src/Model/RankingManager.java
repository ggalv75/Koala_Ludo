package Model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Finishing order and end of game (FR-45, FR-46, FR-47). */
public final class RankingManager {
    private RankingManager() { }

    /**
     * FR-45: when all four pieces of the player are home, the player gets the next free place.
     * Returns true if the player was ranked by this call.
     */
    static boolean rankIfAllPiecesHome(GameState state, Player player) {
        boolean allHome = state.finishedPieceCount(player) == GameRules.PIECES_PER_PLAYER;
        if (player.isFinished() || !allHome) return false;
        player.assignRank(nextFreeRank(state), true);
        return true;
    }

    /** FR-47: the game ends when only one player is still playing. */
    static boolean isGameOver(GameState state) {
        return playersStillPlaying(state).size() <= 1;
    }

    /** FR-47: the player left in the game takes the last place without completing. */
    static void rankRemainingPlayers(GameState state) {
        for (Player player : playersStillPlaying(state)) {
            player.assignRank(nextFreeRank(state), false);
        }
    }

    /** The final ranking table, first place first. Call after every player has a rank. */
    static List<GameResult.Standing> standings(GameState state) {
        List<GameResult.Standing> standings = new ArrayList<>();
        for (Player player : state.players()) {
            standings.add(new GameResult.Standing(player.rank(), player.name(), player.color(),
                    player.completedAllPieces()));
        }
        standings.sort(Comparator.comparingInt(GameResult.Standing::rank));
        return standings;
    }

    private static List<Player> playersStillPlaying(GameState state) {
        return state.players().stream().filter(player -> !player.isFinished()).toList();
    }

    private static int nextFreeRank(GameState state) {
        return state.players().size() - playersStillPlaying(state).size() + 1;
    }
}
