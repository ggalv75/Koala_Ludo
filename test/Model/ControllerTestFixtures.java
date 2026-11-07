package Model;

import java.util.List;

/** Creates repeatable game situations for Controller integration tests. */
public final class ControllerTestFixtures {

    private ControllerTestFixtures() { }

    public static int prepareWinningMove(GameEngine engine) {
        GameState state = engine.state();

        if (state.players().size() != 2) {
            throw new IllegalArgumentException(
                    "This fixture requires a new two-player game");
        }

        Player winner = state.currentPlayer();
        List<Piece> pieces = state.piecesOf(winner);
        int finishStep = state.board().finishStep();

        for (int i = 0; i < pieces.size() - 1; i++) {
            pieces.get(i).moveTo(finishStep);
        }

        Piece lastPiece = pieces.get(pieces.size() - 1);
        lastPiece.moveTo(finishStep - 1);

        DiceRoll roll = new DiceRoll(1);

        state.setLastRoll(roll);
        state.setConsecutiveSixes(0);
        state.setPhase(GameState.Phase.WAITING_FOR_MOVE);

        List<Move> moves = MoveValidator.legalMoves(state, roll);

        if (moves.size() != 1
                || moves.get(0).pieceId() != lastPiece.id()) {
            throw new IllegalStateException(
                    "Expected exactly one winning move");
        }

        state.setLegalMoves(moves);

        return lastPiece.id();
    }
}