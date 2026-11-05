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

    public Board board() { return board; }
    /** FR-06: the level is fixed for the whole game (there is no setter). */
    public Difficulty difficulty() { return board.difficulty(); }
    public List<Player> players() { return players; }
    public List<Piece> pieces() { return pieces; }
    public Player currentPlayer() { return players.get(currentPlayerIndex); }
    public int currentPlayerIndex() { return currentPlayerIndex; }
    public int consecutiveSixes() { return consecutiveSixes; }
    /** The roll of the current turn, or null before the player rolls. */
    public DiceRoll lastRoll() { return lastRoll; }
    public Phase phase() { return phase; }
    public List<Move> legalMoves() { return legalMoves; }
    public GameResult result() { return result; }

    public Piece piece(int id) { return pieces.get(id); }

    public List<Piece> piecesOf(Player player) {
        List<Piece> own = new ArrayList<>();
        for (Piece piece : pieces) if (piece.ownerIndex() == player.index()) own.add(piece);
        return own;
    }

    public boolean isFinished(Piece piece) { return piece.step() >= board.finishStep(); }

    /** The square the piece stands on, or null when it is in the castle or the finish area. */
    public Position positionOf(Piece piece) { return board.positionOf(piece.color(), piece.step()); }

    public int finishedPieceCount(Player player) {
        int count = 0;
        for (Piece piece : piecesOf(player)) if (isFinished(piece)) count++;
        return count;
    }

    public boolean canMove(Piece piece) {
        return legalMoves.stream().anyMatch(move -> move.pieceId() == piece.id());
    }

    /** Latest message for the status line. */
    public String lastMessage() { return log.isEmpty() ? "" : log.peekFirst(); }

    /** Most recent messages first. */
    public List<String> recentLog() { return List.copyOf(log); }

    void setCurrentPlayerIndex(int index) { this.currentPlayerIndex = index; }
    void setConsecutiveSixes(int count) { this.consecutiveSixes = count; }
    void setLastRoll(DiceRoll roll) { this.lastRoll = roll; }
    void setPhase(Phase phase) { this.phase = phase; }
    void setLegalMoves(List<Move> moves) { this.legalMoves = List.copyOf(moves); }
    void setResult(GameResult result) { this.result = result; }

    void log(String message) {
        log.addFirst(message);
        while (log.size() > LOG_SIZE) log.removeLast();
    }
}
