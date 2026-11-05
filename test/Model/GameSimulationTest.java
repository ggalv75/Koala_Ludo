package Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System-level check: complete random Easy games for 2, 3 and 4 players finish correctly and the
 * board stays valid after every action (SDP 5.2.1: acceptance testing, Gal as PO).
 */
class GameSimulationTest {

    @Test
    @DisplayName("Random full games always end with a complete, valid ranking")
    void randomGamesFinish() {
        for (int players = 2; players <= 4; players++) {
            for (long seed = 1; seed <= 5; seed++) {
                playOneGame(players, seed);
            }
        }
    }

    private void playOneGame(int players, long seed) {
        GameEngine engine = new GameEngine(Difficulty.EASY, TestSupport.names(players),
                TestSupport.COLORS.subList(0, players), new Random(seed), new Dice(new Random(seed * 31)),
                Clock.systemUTC());
        int[] gameOverCalls = {0};
        engine.addListener(new GameListener() {
            @Override public void onGameOver(GameResult result) { gameOverCalls[0]++; }
        });
        Random choice = new Random(seed * 7);
        GameState state = engine.state();
        int actions = 0;
        while (state.phase() != GameState.Phase.GAME_OVER) {
            assertTrue(++actions < 100_000, "Game did not end: " + players + " players, seed " + seed);
            if (state.phase() == GameState.Phase.WAITING_FOR_ROLL) {
                engine.rollDice();
            } else {
                List<Move> moves = state.legalMoves();
                assertFalse(moves.isEmpty());
                assertTrue(engine.movePiece(moves.get(choice.nextInt(moves.size())).pieceId()));
            }
            checkInvariants(state);
        }

        String game = players + " players, seed " + seed;
        assertEquals(1, gameOverCalls[0], game);
        List<GameResult.Standing> standings = state.result().standings();
        assertEquals(players, standings.size(), game);
        for (int i = 0; i < players; i++) assertEquals(i + 1, standings.get(i).rank(), game);
        assertEquals(players - 1, standings.stream().filter(GameResult.Standing::completedAllPieces).count(), game);
    }

    /** Pieces stay on their route, and on an unprotected square there is never more than one owner. */
    private void checkInvariants(GameState state) {
        Board board = state.board();
        Map<Position, Set<Integer>> owners = new HashMap<>();
        for (Piece piece : state.pieces()) {
            assertTrue(piece.step() >= Piece.IN_CASTLE && piece.step() <= board.finishStep());
            Position p = state.positionOf(piece);
            if (p != null) owners.computeIfAbsent(p, k -> new HashSet<>()).add(piece.ownerIndex());
        }
        owners.forEach((position, set) -> {
            if (!board.cellAt(position).isProtected()) {
                assertEquals(1, set.size(), "Two owners on an unprotected square " + position);
            }
        });
    }
}
