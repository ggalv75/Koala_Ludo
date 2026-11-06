package Model;

import java.util.List;

/** Turn order and the rule of six: FR-18, FR-19, FR-20 and FR-46. */
public final class TurnManager {
    /** Prevent instantiation of the turn rules helper. */
    private TurnManager() { }

    /** Counts consecutive sixes; returns true for the third six in a row, which is cancelled (FR-20). */
    static boolean registerRoll(GameState state, DiceRoll roll) {
        state.setConsecutiveSixes(roll.isSix() ? state.consecutiveSixes() + 1 : 0);
        return state.consecutiveSixes() >= GameRules.MAX_CONSECUTIVE_SIXES;
    }

    /** FR-19: a six earns another roll; FR-46: not for a player who has just finished. */
    static boolean grantsExtraRoll(GameState state) {
        DiceRoll roll = state.lastRoll();
        return roll != null && roll.isSix() && !state.currentPlayer().isFinished();
    }

    /** FR-18: the turn passes to the next player in order who has not finished (FR-46). */
    static void passTurn(GameState state) {
        List<Player> players = state.players();
        int next = state.currentPlayerIndex();
        for (int i = 0; i < players.size(); i++) {
            next = (next + 1) % players.size();
            if (!players.get(next).isFinished()) break;
        }
        state.setCurrentPlayerIndex(next);
        state.setConsecutiveSixes(0);
        state.setLastRoll(null);
        state.setLegalMoves(List.of());
        state.setPhase(GameState.Phase.WAITING_FOR_ROLL);
    }
}
