package Model;

import java.util.List;

/**
 * Rules of a single turn: what happens after a roll, who rolls again and who plays next
 * (FR-18, FR-19, FR-20, FR-46).
 */
public final class TurnManager {

    /** The four possible results of a roll. */
    enum RollOutcome {
        /** At least one legal move: the player chooses a piece. */
        CHOOSE_PIECE,
        /** No legal move, but a six earns another roll (FR-19). */
        ROLL_AGAIN,
        /** No legal move and no six: the turn passes (FR-18). */
        PASS_TURN,
        /** Third six in a row: the roll is cancelled and the turn passes (FR-20). */
        THIRD_SIX_CANCELLED
    }

    /** Prevent instantiation of the turn rules helper. */
    private TurnManager() { }

    /** Counts consecutive sixes and decides what the player may do with this roll. */
    static RollOutcome afterRoll(GameState state, DiceRoll roll, boolean hasLegalMove) {
        int sixes = roll.isSix() ? state.consecutiveSixes() + 1 : 0;
        state.setConsecutiveSixes(sixes);

        if (sixes >= GameRules.MAX_CONSECUTIVE_SIXES) return RollOutcome.THIRD_SIX_CANCELLED;
        if (hasLegalMove) return RollOutcome.CHOOSE_PIECE;
        if (roll.isSix()) return RollOutcome.ROLL_AGAIN;
        return RollOutcome.PASS_TURN;
    }

    /** FR-19: a six earns another roll after a move. FR-46: not for a player who has just finished. */
    static boolean rollsAgain(Player player, DiceRoll roll) {
        return roll.isSix() && !player.isFinished();
    }

    /**
     * FR-18, FR-46: the turn passes to the next player in order who has not finished.
     * The last roll stays in the state so the die keeps showing it.
     */
    static void passTurn(GameState state) {
        List<Player> players = state.players();
        int next = state.currentPlayerIndex();
        do {
            next = (next + 1) % players.size();
        } while (players.get(next).isFinished() && next != state.currentPlayerIndex());

        state.setCurrentPlayerIndex(next);
        state.setConsecutiveSixes(0);
        state.setLegalMoves(List.of());
        state.setPhase(GameState.Phase.WAITING_FOR_ROLL);
    }
}
