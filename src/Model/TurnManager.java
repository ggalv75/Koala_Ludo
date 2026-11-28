package Model;

import java.util.List;

/**
 * Rules of a single turn: what happens after a roll, who rolls again and who plays next
 * (FR-18 to FR-22, FR-46).
 *
 * <p>The surprise mechanism (SRS 3.2.7) changes turns only through {@link #grantSpecialRoll}
 * (FR-44) and {@link #loseNextTurn} (FR-43). The engine calls {@link #afterMove} only after
 * the effects of the landing square (question, surprise) are done.
 */
public final class TurnManager {

    /** The four possible results of a roll. */
    enum RollOutcome {
        /** At least one legal move: the player chooses a piece. */
        CHOOSE_PIECE,
        /** No legal move, but a six (or an extra roll kept from before) earns another roll (FR-19). */
        ROLL_AGAIN,
        /** No legal move and no six: the turn passes (FR-18). */
        PASS_TURN,
        /** Third six in a row: the roll is cancelled and the turn passes (FR-20). */
        THIRD_SIX_CANCELLED
    }

    /** What the player does after a move has been completed. */
    enum AfterMove {
        /** FR-21: the UltraBoost surprise throw comes next. */
        SPECIAL_ROLL,
        /** FR-19: another regular roll. */
        ROLL_AGAIN,
        /** FR-18: the turn passes to the next player. */
        PASS_TURN
    }

    /** Prevent instantiation of the turn rules helper. */
    private TurnManager() { }

    /** A regular throw: counts toward the six streak. */
    static RollOutcome afterRoll(GameState state, DiceRoll roll, boolean hasLegalMove) {
        return afterRoll(state, roll, hasLegalMove, true);
    }

    /**
     * Decides what the player may do with this roll. Only the step die counts for sixes.
     * When {@code countsTowardStreak} is false (FR-21, the UltraBoost surprise throw) the streak
     * is neither increased nor reset and the throw is never cancelled.
     */
    static RollOutcome afterRoll(GameState state, DiceRoll roll, boolean hasLegalMove, boolean countsTowardStreak) {
        TurnState turn = state.turnState();
        if (countsTowardStreak) {
            int sixes = roll.isSix() ? turn.consecutiveSixes() + 1 : 0;
            turn.setConsecutiveSixes(sixes);
            if (sixes >= GameRules.MAX_CONSECUTIVE_SIXES) return RollOutcome.THIRD_SIX_CANCELLED;
        }
        if (hasLegalMove) return RollOutcome.CHOOSE_PIECE;
        if (roll.isSix() || turn.takeOwedExtraRoll()) return RollOutcome.ROLL_AGAIN;
        return RollOutcome.PASS_TURN;
    }

    /** FR-19: a six earns another roll after a move. FR-46: not for a player who has just finished. */
    static boolean rollsAgain(Player player, DiceRoll roll) {
        return roll.isSix() && !player.isFinished();
    }

    /**
     * What comes after a completed move.
     * <ul>
     *   <li>FR-46: a player who has just finished gets nothing more.</li>
     *   <li>FR-22: a player who lost the turn gets no extra roll, even after a six.</li>
     *   <li>FR-21: a pending UltraBoost surprise throw comes first. If the move itself was made
     *       with a six, that extra roll is kept and comes after the surprise throw (FR-19:
     *       the right to roll again is kept). This order is a team decision; the files do not say it.</li>
     *   <li>FR-19: otherwise a six, or an extra roll kept from before, gives another roll.</li>
     * </ul>
     */
    static AfterMove afterMove(GameState state, Player player, DiceRoll roll) {
        TurnState turn = state.turnState();
        if (player.isFinished() || turn.turnForfeited()) return AfterMove.PASS_TURN;
        if (turn.specialRollPending()) {
            if (roll.isSix()) turn.oweExtraRoll();
            return AfterMove.SPECIAL_ROLL;
        }
        if (rollsAgain(player, roll) || turn.takeOwedExtraRoll()) return AfterMove.ROLL_AGAIN;
        return AfterMove.PASS_TURN;
    }

    /** FR-21, FR-44: the current player's next throw is the special UltraBoost throw, at any level. */
    static void grantSpecialRoll(GameState state) {
        grantSpecialRoll(state, state.currentPlayer());
    }

    /**
     * FR-44, FR-25: the special UltraBoost throw belongs to the owner of the piece that drew it.
     * For the current player it comes right after the move. For another player (a piece moved there
     * in a chain, e.g. by FR-40) it cannot be "immediate" in someone else's turn, so it becomes the
     * first throw of that player's next turn. This is a team decision; the files do not cover it.
     */
    static void grantSpecialRoll(GameState state, Player player) {
        if (player == state.currentPlayer()) {
            state.turnState().setSpecialRollPending(true);
            return;
        }
        state.turnState().deferSpecialRoll(player.index());
        state.log(player.name() + " יקבל/תקבל הטלת UltraBoost מיוחדת בתחילת התור הבא");
    }

    /**
     * FR-22, FR-43: the player loses their next turn. If it is the current player, the turn
     * ends after the move and an extra roll earned with a six is cancelled.
     */
    static void loseNextTurn(GameState state, Player player) {
        state.turnState().loseNextTurn(player.index(), player == state.currentPlayer());
    }

    /**
     * FR-18, FR-46, FR-22: the turn passes to the next player in order who has not finished.
     * A player marked to lose a turn is skipped once. A deferred UltraBoost throw (FR-44) starts the
     * new player's turn. Only the active roll is cleared; the die
     * keeps showing {@link GameState#latestRoll()}.
     */
    static void passTurn(GameState state) {
        List<Player> players = state.players();
        TurnState turn = state.turnState();
        int next = state.currentPlayerIndex();
        // Each finished player is skipped and each lost turn is used up once, so 2n steps always reach a player.
        for (int guard = 0; guard < 2 * players.size(); guard++) {
            next = (next + 1) % players.size();
            Player candidate = players.get(next);
            if (candidate.isFinished()) continue;
            if (turn.takeLostTurn(candidate.index())) {
                // FR-22: a skipped turn also cancels a deferred UltraBoost throw, whatever came first.
                turn.takeDeferredSpecialRoll(candidate.index());
                state.log(candidate.name() + " מפסיד/ה את התור הזה");
                continue;
            }
            break;
        }

        state.setCurrentPlayerIndex(next);
        turn.startNewTurn();
        if (turn.takeDeferredSpecialRoll(next)) turn.setSpecialRollPending(true);
        state.setLastRoll(null);
        state.setLegalMoves(List.of());
        state.setPhase(GameState.Phase.WAITING_FOR_ROLL);
    }
}
