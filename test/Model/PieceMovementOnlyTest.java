package Model;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Focused Easy-level Piece Movement regression tests for SRS FR-23 to FR-27 only.
 * This suite does not test or change the Piece Interaction implementation (FR-28 to FR-32).
 * No external test dependencies are required; execute main().
 */
public final class PieceMovementOnlyTest {
    private static int passed;
    private static int failed;

    /** Run the test cases and report successful assertions. */
    public static void main(String[] args) {
        test("board routes and arrow transition", PieceMovementOnlyTest::boardRoutes);
        test("castle exit requires a six", PieceMovementOnlyTest::castleRequiresSix);
        test("six releases exactly one selected piece", PieceMovementOnlyTest::releaseOnePiece);
        test("release allowed onto own occupied start", PieceMovementOnlyTest::releaseIntoOwnStart);
        test("release allowed onto start with one opponent", PieceMovementOnlyTest::releaseIntoOpponentStart);
        test("normal move follows die", PieceMovementOnlyTest::normalMove);
        test("own arrow enters own home path", PieceMovementOnlyTest::ownArrow);
        test("other colour arrow stays on shared path", PieceMovementOnlyTest::otherArrow);
        test("exact finish roll succeeds", PieceMovementOnlyTest::exactFinish);
        test("overshooting finish is illegal", PieceMovementOnlyTest::overshoot);
        test("invalid move does not change position", PieceMovementOnlyTest::invalidMoveInEngine);
        test("engine release then advance", PieceMovementOnlyTest::engineReleaseAdvance);
        test("no legal move passes turn", PieceMovementOnlyTest::noLegalPassesTurn);
        test("cannot move another player piece", PieceMovementOnlyTest::cannotMoveOtherPlayer);
        test("finished piece cannot move", PieceMovementOnlyTest::finishedPieceCannotMove);
        test("overshoot requires a different eligible piece", PieceMovementOnlyTest::overshootAlternative);
        test("cannot roll twice before selecting a piece", PieceMovementOnlyTest::anotherRollBeforeMoveIsRejected);
        test("engine rejects selection of other player", PieceMovementOnlyTest::inactivePlayerSelectionRejectedByEngine);
        System.out.printf("%nRESULT: passed=%d failed=%d total=%d%n", passed, failed, passed + failed);
        if (failed > 0) System.exit(1);
    }

    /** Run and report one isolated test scenario. */
    private static void test(String name, Runnable scenario) {
        try {
            scenario.run();
            System.out.println("PASS " + name);
            passed++;
        } catch (Throwable ex) {
            System.out.println("FAIL " + name + " -> " + ex);
            ex.printStackTrace(System.out);
            failed++;
        }
    }

    /** Create a configured state for movement rule checks. */
    private static GameState state() {
        List<Player> players = new ArrayList<>();
        PlayerColor[] colors = {PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW};
        for (int i = 0; i < colors.length; i++) players.add(new Player(i, "Player" + i, colors[i]));
        return new GameState(BoardFactory.create(Difficulty.EASY, new Random(2026)), players);
    }

    /** Find the numbered piece used by the test scenario. */
    private static Piece piece(GameState state, int playerIndex, int number) {
        return state.piecesOf(state.players().get(playerIndex)).get(number - 1);
    }

    /** Place a test piece at the requested shared board square. */
    private static void moveToPhysicalSquare(GameState state, Piece piece, Position square) {
        int index = state.board().route(piece.color()).indexOf(square);
        require(index >= 0, "square must be on piece's route");
        piece.moveTo(index);
    }

    /** Find the legal moves for a scripted dice result. */
    private static List<Move> moves(GameState state, int value) {
        return MoveValidator.legalMoves(state, new DiceRoll(value));
    }

    /** Find the candidate move belonging to the requested piece. */
    private static Move moveFor(List<Move> legal, Piece piece) {
        return legal.stream().filter(move -> move.pieceId() == piece.id()).findFirst().orElse(null);
    }

    /** Supply deterministic dice values for repeatable tests. */
    private static Dice scriptedDice(int... values) {
        return new Dice(new Random(0)) {
            private int index = 0;
            /** Produce the next scripted die result. */
            @Override public DiceRoll roll() {
                if (index >= values.length) throw new AssertionError("test script depleted");
                return new DiceRoll(values[index++]);
            }
        };
    }

    /** Create a deterministic game engine for the test scenario. */
    private static GameEngine engine(int... rolls) {
        return new GameEngine(Difficulty.EASY, List.of("Adi", "Dana"),
                List.of(PlayerColor.RED, PlayerColor.GREEN),
                new Random(123), scriptedDice(rolls), Clock.systemUTC());
    }

    /** Assert that actual and expected values are equal. */
    private static void equal(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual))
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
    }

    /** Fail the test when its required condition is not satisfied. */
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** Verify the geometry of colored routes around the Easy board. */
    private static void boardRoutes() {
        Board board = state().board();
        equal(15, board.size(), "size");
        equal(56, board.finishStep(), "finish step");
        for (PlayerColor color : PlayerColor.values()) {
            equal(56, board.route(color).size(), "route length for " + color);
            equal(CellType.START, board.cellAt(color, 0).type(), "start");
            equal(color, board.cellAt(color, 0).color(), "start color");
            equal(CellType.ARROW, board.cellAt(color, 50).type(), "arrow");
            equal(color, board.cellAt(color, 50).color(), "arrow color");
            equal(CellType.HOME, board.cellAt(color, 51).type(), "first home square");
            require(board.positionOf(color, 56) == null, "finish is not a track square");
        }
    }

    /** Exercise castle requires six and check its expected outcome. */
    private static void castleRequiresSix() {
        GameState state = state();
        equal(0, moves(state, 1).size(), "one cannot release");
        equal(0, moves(state, 5).size(), "five cannot release");
        equal(4, moves(state, 6).size(), "all four pieces can be chosen for release");
    }

    /** Exercise release one piece and check its expected outcome. */
    private static void releaseOnePiece() {
        GameState state = state();
        Piece red1 = piece(state, 0, 1);
        Move chosen = moveFor(moves(state, 6), red1);
        require(chosen != null && chosen.release(), "one piece may be released");
        equal(-1, chosen.fromStep(), "castle step");
        equal(0, chosen.toStep(), "start step");
        equal(0, chosen.steps(), "release consumes the roll as an action, not six track squares");
    }

    /** Exercise release into own start and check its expected outcome. */
    private static void releaseIntoOwnStart() {
        GameState state = state();
        piece(state, 0, 1).moveTo(0);
        require(moveFor(moves(state, 6), piece(state, 0, 2)) != null, "own piece on start must not block");
    }

    /** Exercise release into opponent start and check its expected outcome. */
    private static void releaseIntoOpponentStart() {
        GameState state = state();
        moveToPhysicalSquare(state, piece(state, 1, 1), state.board().positionOf(PlayerColor.RED, 0));
        require(moveFor(moves(state, 6), piece(state, 0, 1)) != null, "one opponent on protected start may coexist");
    }

    /** Exercise normal move and check its expected outcome. */
    private static void normalMove() {
        GameState state = state();
        Piece red1 = piece(state, 0, 1);
        red1.moveTo(3);
        Move choice = moveFor(moves(state, 4), red1);
        require(choice != null && !choice.partial(), "full move exists");
        equal(7, choice.toStep(), "advance by die value");
    }

    /** Exercise own arrow and check its expected outcome. */
    private static void ownArrow() {
        GameState state = state();
        Piece red = piece(state, 0, 1);
        red.moveTo(49);
        equal(50, moveFor(moves(state, 1), red).toStep(), "arrive at arrow");
        red.moveTo(50);
        equal(51, moveFor(moves(state, 1), red).toStep(), "enter colored home path");
        equal(CellType.HOME, state.board().cellAt(red.color(), 51).type(), "first home square after arrow");
    }

    /** Exercise other arrow and check its expected outcome. */
    private static void otherArrow() {
        GameState state = state();
        Piece red = piece(state, 0, 1);
        Position otherArrow = state.board().route(PlayerColor.GREEN).get(50);
        int index = state.board().route(PlayerColor.RED).indexOf(otherArrow);
        require(index >= 0 && index < 50, "red visits green arrow on shared path");
        red.moveTo(index);
        equal(index + 1, moveFor(moves(state, 1), red).toStep(), "no home-path diversion on other arrow");
    }

    /** Exercise exact finish and check its expected outcome. */
    private static void exactFinish() {
        GameState state = state();
        Piece red = piece(state, 0, 1);
        red.moveTo(52);
        Move result = moveFor(moves(state, 4), red);
        require(result != null, "exact finish is legal");
        equal(56, result.toStep(), "finished position");
    }

    /** Exercise overshoot and check its expected outcome. */
    private static void overshoot() {
        GameState state = state();
        piece(state, 0, 1).moveTo(55);
        equal(null, moveFor(moves(state, 2), piece(state, 0, 1)), "cannot overshoot finished position");
    }

    /** Exercise invalid move in engine and check its expected outcome. */
    private static void invalidMoveInEngine() {
        GameEngine engine = engine(6);
        require(!engine.movePiece(0), "cannot move before rolling");
        equal(Piece.IN_CASTLE, engine.state().piece(0).step(), "position unchanged");
        engine.rollDice();
        require(!engine.movePiece(1000), "unknown piece cannot move");
        equal(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase(), "still waiting for valid action");
        equal(Piece.IN_CASTLE, engine.state().piece(0).step(), "position still unchanged");
    }

    /** Exercise engine release advance and check its expected outcome. */
    private static void engineReleaseAdvance() {
        GameEngine engine = engine(6, 3);
        engine.rollDice();
        require(engine.movePiece(0), "release succeeds");
        equal(0, engine.state().piece(0).step(), "piece on start");
        equal(0, engine.state().currentPlayerIndex(), "six allows another roll");
        engine.rollDice();
        require(engine.movePiece(0), "advance succeeds");
        equal(3, engine.state().piece(0).step(), "die value applied once to one piece");
        equal(1, engine.state().currentPlayerIndex(), "turn advanced");
    }

    /** Exercise no legal passes turn and check its expected outcome. */
    private static void noLegalPassesTurn() {
        GameEngine engine = engine(3);
        engine.rollDice();
        equal(1, engine.state().currentPlayerIndex(), "no legal move -> next player");
        equal(GameState.Phase.WAITING_FOR_ROLL, engine.state().phase(), "next player may roll");
    }

    /** Exercise cannot move other player and check its expected outcome. */
    private static void cannotMoveOtherPlayer() {
        GameState state = state();
        piece(state, 1, 1).moveTo(4);
        Piece red = piece(state, 0, 1);
        red.moveTo(4);
        require(moveFor(moves(state, 2), red) != null, "active red should have a legal move");
        require(moveFor(moves(state, 2), piece(state, 1, 1)) == null,
                "a green tool cannot be moved on red player's turn");
    }

    /** Exercise finished piece cannot move and check its expected outcome. */
    private static void finishedPieceCannotMove() {
        GameState state = state();
        Piece red = piece(state, 0, 1);
        red.moveTo(state.board().finishStep());
        require(moveFor(moves(state, 1), red) == null, "finished pieces stay finished");
    }

    /** Exercise overshoot alternative and check its expected outcome. */
    private static void overshootAlternative() {
        GameState state = state();
        Piece overshooter = piece(state, 0, 1);
        Piece eligible = piece(state, 0, 2);
        overshooter.moveTo(55);
        eligible.moveTo(42);
        List<Move> list = moves(state, 2);
        require(moveFor(list, overshooter) == null, "overshooting piece must not be offered");
        equal(44, moveFor(list, eligible).toStep(), "other piece can still use the die");
    }

    /** Exercise another roll before move is rejected and check its expected outcome. */
    private static void anotherRollBeforeMoveIsRejected() {
        GameEngine engine = engine(6, 2);
        engine.rollDice();
        equal(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase(), "must select piece");
        engine.rollDice();
        equal(6, engine.state().lastRoll().value(), "illegal repeat roll does not replace result");
        equal(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase(), "still waiting for move");
    }

    /** Exercise inactive player selection rejected by engine and check its expected outcome. */
    private static void inactivePlayerSelectionRejectedByEngine() {
        GameEngine engine = engine(6);
        engine.rollDice();
        require(!engine.movePiece(4), "green piece cannot be chosen in red turn");
        equal(Piece.IN_CASTLE, engine.state().piece(4).step(), "other player's position unchanged");
        equal(GameState.Phase.WAITING_FOR_MOVE, engine.state().phase(), "valid red move still required");
    }
}
