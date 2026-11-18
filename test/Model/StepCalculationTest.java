package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FR-16, FR-17 with FR-24 and FR-26: how many squares each piece may move with a multiplier die.
 * Easy board route of a colour: steps 0-50 shared track, 51-55 home path, 56 finish.
 * Owner: Gal (iteration 2).
 */
class StepCalculationTest {

    private GameState state;
    private Board board;
    private Piece first;
    private Piece second;

    @BeforeEach
    void setUp() {
        state = TestSupport.newState(2);
        board = state.board();
        List<Piece> galPieces = state.piecesOf(state.players().get(0));
        first = galPieces.get(0);
        second = galPieces.get(1);
    }

    @Test
    @DisplayName("Test setup: home path starts at 51 and the finish is 56 on the Easy board")
    void routeLayoutAssumedByTheseTests() {
        assertEquals(51, board.homePathStart());
        assertEquals(56, board.finishStep());
    }

    @Test
    @DisplayName("FR-16: on the track the piece moves die x (N - 1): 4 with Boost 3 is 8 squares")
    void trackPieceMovesDieTimesCoefficient() {
        first.moveTo(10);
        assertEquals(Optional.of(18), targetOf(first, roll(4, MultiplierDie.BOOST, 3)));
    }

    @Test
    @DisplayName("FR-16: UltraBoost 6 gives coefficient 5, so 3 becomes 15 squares")
    void ultraBoostSixGivesCoefficientFive() {
        first.moveTo(10);
        assertEquals(Optional.of(25), targetOf(first, roll(3, MultiplierDie.ULTRA_BOOST, 6)));
    }

    @Test
    @DisplayName("FR-16 boundary: multiplier 1 gives 0 squares, so a track piece has no move")
    void coefficientZeroFreezesTrackPiece() {
        first.moveTo(10);
        assertTrue(MoveValidator.legalMoves(state, roll(4, MultiplierDie.BOOST, 1)).isEmpty());
    }

    @Test
    @DisplayName("FR-24: a six on the step die still releases a piece when the coefficient is 0")
    void sixReleasesEvenWithCoefficientZero() {
        first.moveTo(10);
        List<Move> moves = MoveValidator.legalMoves(state, roll(6, MultiplierDie.BOOST, 1));
        assertFalse(moves.isEmpty());
        assertTrue(moves.stream().allMatch(Move::release), "Only releases; the track piece moves 0 squares");
    }

    @Test
    @DisplayName("FR-17: a piece already on the home path moves by the step die only")
    void homePathPieceIgnoresMultiplier() {
        first.moveTo(51);
        assertEquals(Optional.of(53), targetOf(first, roll(2, MultiplierDie.BOOST, 3)));
    }

    @Test
    @DisplayName("FR-17: coefficient 0 does not stop a piece on the home path")
    void coefficientZeroDoesNotFreezeHomePath() {
        first.moveTo(53);
        assertEquals(Optional.of(55), targetOf(first, roll(2, MultiplierDie.BOOST, 1)));
    }

    @Test
    @DisplayName("FR-17: with pieces on the track and on the home path the player chooses between both counts")
    void playerChoosesBetweenTrackAndHomePath() {
        first.moveTo(51);
        second.moveTo(10);
        DiceRoll roll = roll(2, MultiplierDie.ULTRA_BOOST, 4);
        assertEquals(Optional.of(53), targetOf(first, roll), "home path: die only");
        assertEquals(Optional.of(16), targetOf(second, roll), "track: 2 x 3");
    }

    @Test
    @DisplayName("FR-16, FR-17: a track piece that enters the home path still uses the multiplier")
    void enteringTheHomePathUsesTheMultiplier() {
        first.moveTo(48);
        assertEquals(Optional.of(52), targetOf(first, roll(2, MultiplierDie.BOOST, 3)));
    }

    @Test
    @DisplayName("FR-26: the multiplied count must land exactly on the finish")
    void exactFinishWithMultiplier() {
        first.moveTo(48);
        assertEquals(Optional.of(56), targetOf(first, roll(4, MultiplierDie.BOOST, 3)), "8 squares: exact");
        assertEquals(Optional.empty(), targetOf(first, roll(5, MultiplierDie.BOOST, 3)), "10 squares: too far");
    }

    @Test
    @DisplayName("FR-26: on the home path the die alone must fit the distance to the finish")
    void exactFinishOnHomePath() {
        first.moveTo(53);
        assertEquals(Optional.of(56), targetOf(first, roll(3, MultiplierDie.ULTRA_BOOST, 6)));
        assertEquals(Optional.empty(), targetOf(first, roll(4, MultiplierDie.ULTRA_BOOST, 1)));
    }

    @Test
    @DisplayName("Regression, Easy: without a multiplier die the piece moves by the die value")
    void easyRollIsUnchanged() {
        first.moveTo(10);
        assertEquals(Optional.of(14), targetOf(first, new DiceRoll(4)));
    }

    private static DiceRoll roll(int die, MultiplierDie multiplier, int value) {
        return new DiceRoll(die, multiplier, value);
    }

    private Optional<Integer> targetOf(Piece piece, DiceRoll roll) {
        return MoveValidator.legalMoves(state, roll).stream()
                .filter(move -> move.pieceId() == piece.id())
                .map(Move::toStep)
                .findFirst();
    }
}
