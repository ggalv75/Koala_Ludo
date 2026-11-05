package Model;

/**
 * A legal move for one piece. {@code release} is leaving the castle (FR-24);
 * {@code partial} means the piece stops right before an opponent's Double Block (FR-31).
 */
public record Move(int pieceId, int fromStep, int toStep, boolean release, boolean partial) {
    public int steps() { return release ? 0 : toStep - fromStep; }
}
