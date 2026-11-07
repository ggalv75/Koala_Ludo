package Model;

/**
 * Rules that are the same at every difficulty level (Ludo Project-V2, pp. 3-5).
 * Values that change between levels live in {@link Difficulty} (SRS NFR-05).
 */
public final class GameRules {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;
    /** FR-04: every player has exactly four pieces. */
    public static final int PIECES_PER_PLAYER = 4;
    /** FR-15: the regular step die has six faces. */
    public static final int DIE_FACES = 6;
    /** FR-24 / FR-19: a six releases a piece from the castle and grants another roll. */
    public static final int SIX = 6;
    /** FR-20: the third six in a row is cancelled and the turn passes. */
    public static final int MAX_CONSECUTIVE_SIXES = 3;
    /** Keeps names readable on the board and results screens. */
    public static final int MAX_NAME_LENGTH = 15;

    /** Prevent instantiation of the shared rule constants. */
    private GameRules() { }
}
