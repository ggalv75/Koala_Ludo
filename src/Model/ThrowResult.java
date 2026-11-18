package Model;

import java.util.List;
import java.util.Objects;

/** What one throw produced: the dice values, the legal moves and what the player may do next. */
record ThrowResult(DiceRoll roll, List<Move> legalMoves, TurnManager.RollOutcome outcome) {
    /** Keep an immutable copy of the moves. */
    ThrowResult {
        Objects.requireNonNull(roll, "roll");
        legalMoves = List.copyOf(legalMoves);
        Objects.requireNonNull(outcome, "outcome");
    }
}
