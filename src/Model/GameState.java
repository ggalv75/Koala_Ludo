package Model;

import java.util.*;

/**
 * Everything the screens need to show the current game. Public methods are read-only;
 * only classes of the Model package change the state.
 */
public final class GameState {
    public enum Phase { WAITING_FOR_ROLL, WAITING_FOR_MOVE, GAME_OVER }

    private static final int LOG_SIZE = 40;

    private final Board board;
    private final List<Player> players;
    private final List<Piece> pieces;
    private int currentPlayerIndex;
    private int consecutiveSixes;
    private DiceRoll lastRoll;
    private DiceRoll latestRoll;
    private Phase phase = Phase.WAITING_FOR_ROLL;
    private List<Move> legalMoves = List.of();
    private final Deque<String> log = new ArrayDeque<>();
    private GameResult result;

    /** FR-04: four pieces per player, all starting in the owner's castle. */
    GameState(Board board, List<Player> players) {
        this.board = Objects.requireNonNull(board);
        this.players = List.copyOf(players);
        List<Piece> created = new ArrayList<>();
        for (Player player : this.players) {
            for (int n = 1; n <= GameRules.PIECES_PER_PLAYER; n++) {
                created.add(new Piece(created.size(), player.index(), n, player.color()));
            }
        }
        this.pieces = List.copyOf(created);
    }

    /** Return the board used by the current game. */
    public Board board() { return board; }
    /** FR-06: the level is fixed for the whole game (there is no setter). */
    public Difficulty difficulty() { return board.difficulty(); }
    /** Return the immutable list of participating players. */
    public List<Player> players() { return players; }
    /** Return every piece in the current game. */
    public List<Piece> pieces() { return pieces; }
    /** Return the player whose turn is active. */
    public Player currentPlayer() { return players.get(currentPlayerIndex); }
    /** Return the index of the active player. */
    public int currentPlayerIndex() { return currentPlayerIndex; }
    /** Return the current streak of successive sixes. */
    public int consecutiveSixes() { return consecutiveSixes; }
    /** Return the roll awaiting a piece move, or null when no roll is active. */
    public DiceRoll lastRoll() { return lastRoll; }
    /** Return the last actual die result, including between turns. */
    public DiceRoll latestRoll() { return latestRoll; }
    /** Return the current phase of the game turn. */
    public Phase phase() { return phase; }
    /** Return the immutable set of currently available moves. */
    public List<Move> legalMoves() { return legalMoves; }
    /** Return the finished game result when available. */
    public GameResult result() { return result; }

    /** Return one piece by its stable identifier. */
    public Piece piece(int id) { return pieces.get(id); }

    /** Return all pieces belonging to the requested player. */
    public List<Piece> piecesOf(Player player) {
        List<Piece> own = new ArrayList<>();
        for (Piece piece : pieces) if (piece.ownerIndex() == player.index()) own.add(piece);
        return own;
    }

    /** Check whether a piece has reached the final finish position. */
    public boolean isFinished(Piece piece) { return piece.step() >= board.finishStep(); }

    /** The square the piece stands on, or null when it is in the castle or the finish area. */
    public Position positionOf(Piece piece) { return board.positionOf(piece.color(), piece.step()); }

    /** Count a player’s pieces already inside the finish. */
    public int finishedPieceCount(Player player) {
        int count = 0;
        for (Piece piece : piecesOf(player)) if (isFinished(piece)) count++;
        return count;
    }

    /** Check whether a piece has a legal move in this phase. */
    public boolean canMove(Piece piece) {
        return legalMoves.stream().anyMatch(move -> move.pieceId() == piece.id());
    }

    /** Latest message for the status line. */
    public String lastMessage() { return log.isEmpty() ? "" : log.peekFirst(); }

    /** Most recent messages first. */
    public List<String> recentLog() { return List.copyOf(log); }

    /** Update the active player as part of turn management. */
    void setCurrentPlayerIndex(int index) { this.currentPlayerIndex = index; }
    /** Update the current six-roll streak. */
    void setConsecutiveSixes(int count) { this.consecutiveSixes = count; }
    /** Update the active roll and retain non-null results for the persistent die display. */
    void setLastRoll(DiceRoll roll) {
        this.lastRoll = roll;
        if (roll != null) this.latestRoll = roll;
    }
    /** Change the active phase of the game. */
    void setPhase(Phase phase) { this.phase = phase; }
    /** Store an immutable copy of the current legal moves. */
    void setLegalMoves(List<Move> moves) { this.legalMoves = List.copyOf(moves); }
    /** Store the final outcome after the game ends. */
    void setResult(GameResult result) { this.result = result; }

    /** Append a recent message while enforcing the history limit. */
    void log(String message) {
        log.addFirst(message);
        while (log.size() > LOG_SIZE) log.removeLast();
    }
}
