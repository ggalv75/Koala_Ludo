package Model;

/**
 * Observer design pattern: views register with the {@link GameEngine} and are told when the
 * game changes, so the Model never depends on the View (MVC, SRS 3.4.2).
 */
public interface GameListener {
    /** A legal piece movement was committed. Fired just before onStateChanged.
     *  Views may use the immutable Move to animate the path; it changes no game rules. */
    default void onPieceMoved(Move move) { }

    default void onStateChanged(GameState state) { }

    default void onGameOver(GameResult result) { }
}
