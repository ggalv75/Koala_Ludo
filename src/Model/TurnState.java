package Model;

import java.util.HashSet;
import java.util.Set;

/**
 * Turn bookkeeping that is not part of the board: the six streak (FR-20), a pending
 * UltraBoost surprise throw (FR-21), an extra roll kept for later (FR-19) and players who lose
 * their next turn (FR-22). Changed only through {@link TurnManager}.
 */
final class TurnState {
    private int consecutiveSixes;
    private boolean specialRollPending;
    private boolean extraRollOwed;
    private boolean turnForfeited;
    private final Set<Integer> playersLosingNextTurn = new HashSet<>();

    /** Current streak of sixes on the step die. */
    int consecutiveSixes() { return consecutiveSixes; }
    /** Update the six streak after a regular throw. */
    void setConsecutiveSixes(int count) { consecutiveSixes = count; }

    /** FR-21: the next throw of the current player is the UltraBoost surprise throw. */
    boolean specialRollPending() { return specialRollPending; }
    /** Set or clear the pending UltraBoost surprise throw. */
    void setSpecialRollPending(boolean pending) { specialRollPending = pending; }

    /** Returns true once if the special throw was pending, and clears it. */
    boolean takeSpecialRoll() {
        boolean pending = specialRollPending;
        specialRollPending = false;
        return pending;
    }

    /** FR-19: a six whose extra roll waits until the surprise throw is done. */
    void oweExtraRoll() { extraRollOwed = true; }

    /** Returns true once if an extra roll is owed, and clears it. */
    boolean takeOwedExtraRoll() {
        boolean owed = extraRollOwed;
        extraRollOwed = false;
        return owed;
    }

    /** FR-22: the current player lost the turn, so any extra roll in this turn is cancelled. */
    boolean turnForfeited() { return turnForfeited; }

    /** FR-22: mark a player to lose their next turn; ends the current turn if it is theirs. */
    void loseNextTurn(int playerIndex, boolean isCurrentPlayer) {
        playersLosingNextTurn.add(playerIndex);
        if (isCurrentPlayer) {
            turnForfeited = true;
            specialRollPending = false;
            extraRollOwed = false;
        }
    }

    /** Check whether a player is waiting to lose a turn. */
    boolean losesNextTurn(int playerIndex) { return playersLosingNextTurn.contains(playerIndex); }

    /** Returns true once for a player who loses this turn, and clears the penalty. */
    boolean takeLostTurn(int playerIndex) { return playersLosingNextTurn.remove(playerIndex); }

    /** A new turn starts: everything that belonged to the previous player's turn is cleared. */
    void startNewTurn() {
        consecutiveSixes = 0;
        specialRollPending = false;
        extraRollOwed = false;
        turnForfeited = false;
    }
}
