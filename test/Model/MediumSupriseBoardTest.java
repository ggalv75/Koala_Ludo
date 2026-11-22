package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Tests surprise placement and activation on the real Medium board. */
class MediumSurpriseBoardTest {

    @Test
    void easyKeepsItsExistingSafeSquaresWithoutSurprises() {
        Board board = BoardFactory.create(Difficulty.EASY, new Random(42));
        assertEquals(0, board.count(CellType.SURPRISE));
        assertEquals(4, board.count(CellType.SAFE));
    }

    @Test
    void mediumHasTwoSurprisesInAdditionToEightSafeSquares() {
        for (int seed = 0; seed < 100; seed++) {
            Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(seed));
            assertEquals(2, board.count(CellType.SURPRISE), "Seed: " + seed);
            assertEquals(8, board.count(CellType.SAFE), "Seed: " + seed);
        }
    }

    @Test
    void specialSquaresUseOnlyFreeRegularCellsAndPreserveFixedCells() {
        Map<Position, Cell> fixed = new MediumBoardLayout().fixedCells();

        for (int seed = 0; seed < 100; seed++) {
            Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(seed));

            for (Map.Entry<Position, Cell> entry : board.cells().entrySet()) {
                Cell original = fixed.get(entry.getKey());
                Cell actual = entry.getValue();

                if (actual.type() == CellType.SURPRISE
                        || actual.type() == CellType.SAFE) {
                    assertEquals(CellType.REGULAR, original.type());
                    assertNull(original.color());
                }

                if (original.type() != CellType.REGULAR
                        || original.color() != null) {
                    assertEquals(original, actual,
                            "A fixed cell was changed: " + entry.getKey());
                }
            }
        }
    }

    @Test
    void theSameRandomSeedProducesTheSameBoard() {
        Board first = BoardFactory.create(Difficulty.MEDIUM, new Random(73));
        Board second = BoardFactory.create(Difficulty.MEDIUM, new Random(73));
        assertEquals(first.cells(), second.cells());
    }

    @Test
    void surprisePositionsVaryAcrossDifferentSeeds() {
        Set<Set<Position>> layouts = new HashSet<>();
        for (int seed = 0; seed < 30; seed++) {
            Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(seed));
            Set<Position> surprises = new HashSet<>();
            board.cells().forEach((position, cell) -> {
                if (cell.type() == CellType.SURPRISE) surprises.add(position);
            });
            layouts.add(Set.copyOf(surprises));
        }
        assertTrue(layouts.size() > 1,
                "Surprises must not always occupy the same positions");
    }

    @Test
    void landingOnARealMediumSurpriseActivatesTheEngine() {
        Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(42));
        int destination = -1;

        for (int step = 1; step < board.homePathStart(); step++) {
            if (board.cellAt(PlayerColor.RED, step).type() == CellType.SURPRISE) {
                destination = step;
                break;
            }
        }
        assertTrue(destination > 0, "The real route must contain a surprise");

        Dice dice = new Dice(new Random(1)) {
            @Override public DiceRoll roll() { return new DiceRoll(1); }
            @Override public int rollMultiplier(MultiplierDie die) {
                assertEquals(MultiplierDie.BOOST, die);
                return 2;
            }
        };
        Random surpriseRandom = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) {
                assertEquals(100, bound);
                return 20; // Return to castle, independent of chance.
            }
        };
        GameEngine engine = new GameEngine(board,
                List.of("Dor", "Gal"), List.of(PlayerColor.RED, PlayerColor.GREEN),
                dice, Clock.systemUTC(), new SurpriseSelector(surpriseRandom));

        List<Move> moves = new ArrayList<>();
        List<SurpriseEvent> events = new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onPieceMoved(Move move) { moves.add(move); }
            @Override public void onSurpriseDrawn(SurpriseEvent event) { events.add(event); }
        });

        engine.state().piece(0).moveTo(destination - 1);
        engine.rollDice();
        assertTrue(engine.movePiece(0));

        assertEquals(1, events.size());
        assertEquals(SurpriseType.RETURN_TO_CASTLE, events.get(0).type());
        assertEquals(2, moves.size());
        assertEquals(destination, moves.get(0).toStep());
        assertEquals(Piece.IN_CASTLE, moves.get(1).toStep());
        assertTrue(engine.state().piece(0).isInCastle());
        assertEquals(1, engine.state().currentPlayerIndex());
    }
}

