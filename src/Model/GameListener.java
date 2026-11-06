package Model;

/** Observer events isolate game rules from animations (MVC, SRS 3.4.2). */
public interface GameListener {
    /** Notify listeners about a committed piece movement before the state refresh. */
    default void onPieceMoved(Move move) { }

    /** Notify listeners of capture, protection, block and partial-move outcomes. */
    default void onPieceInteraction(PieceInteractionEvent event) { }

    /** Announce a player who finished all four pieces and received a final place. */
    default void onPlayerFinished(PlayerFinishEvent event) { }

    /** Notify listeners that the current game state has changed. */
    default void onStateChanged(GameState state) { }

    /** Notify listeners that the game finished and a result is available. */
    default void onGameOver(GameResult result) { }
}
