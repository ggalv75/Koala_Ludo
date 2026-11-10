package Model;

import java.time.Clock;
import java.util.*;

/** Repeatable FR-23–FR-32 tests for Easy-level Piece Movement and Interaction. */
public final class PieceInteractionTest {
    private static int passed;
    private static int failed;

    /** Build a deterministic engine with two players and scripted die rolls. */
    private static GameEngine engine(int... rolls) {
        Dice die = new Dice(new Random(2)) {
            private int index;
            /** Return the next scripted value for a reproducible test. */
            @Override public DiceRoll roll() { return new DiceRoll(rolls[index++]); }
        };
        return new GameEngine(Difficulty.EASY, List.of("Red", "Green"),
                List.of(PlayerColor.RED, PlayerColor.GREEN), new Random(23), die, Clock.systemUTC());
    }

    /** Locate the position of another player's piece on the same physical square. */
    private static void placeAt(GameState state, Piece piece, Position square) {
        int step = state.board().route(piece.color()).indexOf(square);
        require(step >= 0, "square must lie on owner's shared route: " + square);
        piece.moveTo(step);
    }

    /** Find a regular common-track square reachable by Red from a valid positive step. */
    private static int regularRedTarget(GameState state) {
        for (int step = 6; step < state.board().homePathStart() - 6; step++) {
            Position pos = state.board().positionOf(PlayerColor.RED, step);
            if (state.board().cellAt(pos).type() == CellType.REGULAR
                    && state.board().route(PlayerColor.GREEN).contains(pos)) return step;
        }
        throw new AssertionError("No regular shared target found");
    }

    /** Throw an assertion failure if a required property is false. */
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Run one named scenario and remember its outcome. */
    private static void test(String name, Runnable scenario) {
        try {
            scenario.run();
            passed++;
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            failed++;
            System.err.println("FAIL " + name + ": " + failure);
            failure.printStackTrace();
        }
    }

    /** Capture the last semantic interaction emitted by one game engine. */
    private static List<PieceInteractionEvent> observe(GameEngine engine) {
        List<PieceInteractionEvent> events = new ArrayList<>();
        engine.addListener(new GameListener() {
            /** Collect immutable interaction events without changing gameplay. */
            @Override public void onPieceInteraction(PieceInteractionEvent event) { events.add(event); }
        });
        return events;
    }

    /** Verify all interactions and the precedence of full moves over partial moves. */
    public static void main(String[] args) {
        test("FR-28 landing on a regular opponent captures its only pawn", () -> {
            GameEngine e = engine(3);
            GameState s = e.state();
            int target = regularRedTarget(s);
            s.piece(0).moveTo(target - 3);
            placeAt(s, s.piece(4), s.board().positionOf(PlayerColor.RED, target));
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice();
            require(e.movePiece(0), "capture move legal");
            require(s.piece(4).isInCastle(), "victim returned to castle");
            require(s.piece(0).step() == target, "attacker stays at target");
            require(events.size() == 1 && events.get(0).capture() != null, "capture event emitted");
            require(events.get(0).capture().pieceId() == 4, "victim id recorded");
            require(events.get(0).capture().square().equals(s.board().positionOf(PlayerColor.RED, target)),
                    "capture square recorded before reset");
        });
        test("FR-28 landing on empty regular square does not capture", () -> {
            GameEngine e = engine(3);
            e.state().piece(0).moveTo(2);
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice();
            require(e.movePiece(0), "movement legal");
            require(events.stream().noneMatch(event -> event.capture() != null), "no phantom capture");
        });
        test("FR-29 opponent shares protected start and survives", () -> {
            GameEngine e = engine(6);
            GameState s = e.state();
            placeAt(s, s.piece(4), s.board().positionOf(PlayerColor.RED, 0));
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice();
            require(e.movePiece(0), "release onto protected occupied start legal");
            require(!s.piece(4).isInCastle(), "no capture on protected start");
            require(events.size() == 1 && events.get(0).protectedLanding() != null,
                    "shield effect announced");
        });
        test("FR-29 opponent shares safe square and survives", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            int end = -1;
            for (int k = 2; k < s.board().homePathStart() - 2; k++) {
                Position p = s.board().positionOf(PlayerColor.RED, k);
                if (s.board().cellAt(p).type() == CellType.SAFE
                        && s.board().route(PlayerColor.GREEN).contains(p)) { end = k; break; }
            }
            require(end >= 0, "shared safe square available");
            s.piece(0).moveTo(end - 2);
            placeAt(s, s.piece(4), s.board().positionOf(PlayerColor.RED, end));
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice(); require(e.movePiece(0), "move onto safe square legal");
            require(!s.piece(4).isInCastle(), "safe piece cannot be captured");
            require(events.size() == 1 && events.get(0).protectedLanding() != null, "shield event");
        });
        test("FR-30 forming a block emits a created event", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            s.piece(0).moveTo(0); s.piece(1).moveTo(2);
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice(); require(e.movePiece(0), "join second pawn");
            require(InteractionRules.blockOwners(s, s.positionOf(s.piece(0))).contains(0), "block active");
            require(events.size() == 1 && events.get(0).blockChanges().stream()
                    .anyMatch(change -> change.ownerIndex() == 0 && change.formed()), "formation announced");
        });
        test("FR-32 moving out of a block emits a removed event", () -> {
            GameEngine e = engine(1);
            GameState s = e.state();
            s.piece(0).moveTo(2); s.piece(1).moveTo(2);
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice(); require(e.movePiece(0), "leave own block");
            require(!InteractionRules.isDoubleBlock(s, s.positionOf(s.piece(1))), "block removed");
            require(events.size() == 1 && events.get(0).blockChanges().stream()
                    .anyMatch(change -> change.ownerIndex() == 0 && !change.formed()), "removal announced");
        });
        test("FR-30 three owned pawns still create a block", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            s.piece(0).moveTo(0); s.piece(1).moveTo(2); s.piece(2).moveTo(2);
            e.rollDice(); require(e.movePiece(0), "third pawn joins group");
            require(InteractionRules.blockOwners(s, s.positionOf(s.piece(0))).contains(0), "3 pawns are a block");
        });
        test("FR-31 opponent blocks an intermediate square; move is partial", () -> {
            GameEngine e = engine(4);
            GameState s = e.state();
            s.piece(0).moveTo(2);
            Position wall = s.board().positionOf(PlayerColor.RED, 4);
            placeAt(s, s.piece(4), wall); placeAt(s, s.piece(5), wall);
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice();
            require(s.legalMoves().size() == 1 && s.legalMoves().get(0).partial(), "partial move offered");
            require(e.movePiece(0), "partial move executed");
            require(s.piece(0).step() == 3, "stopped immediately before wall");
            require(events.size() == 1 && wall.equals(events.get(0).blockedBy()), "wall effect announced");
        });
        test("FR-31 full alternative takes precedence over every partial candidate", () -> {
            GameEngine e = engine(4);
            GameState s = e.state();
            s.piece(0).moveTo(2); s.piece(1).moveTo(10);
            Position wall = s.board().positionOf(PlayerColor.RED, 4);
            placeAt(s, s.piece(4), wall); placeAt(s, s.piece(5), wall);
            e.rollDice();
            require(s.legalMoves().stream().noneMatch(m -> m.pieceId() == 0), "partial hidden");
            require(s.legalMoves().stream().anyMatch(m -> m.pieceId() == 1 && !m.partial()), "full available");
            require(!e.movePiece(0), "blocked partial cannot be chosen");
            require(s.piece(0).step() == 2, "no movement for refused candidate");
        });
        test("FR-31 cannot land directly on a double block", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            s.piece(0).moveTo(2);
            Position wall = s.board().positionOf(PlayerColor.RED, 4);
            placeAt(s, s.piece(4), wall); placeAt(s, s.piece(5), wall);
            e.rollDice();
            require(s.legalMoves().stream().anyMatch(m -> m.toStep() == 3 && m.partial()),
                    "must stop at step before occupied destination");
        });
        test("FR-31 immediately adjacent opponent block prevents moving", () -> {
            GameEngine e = engine(1);
            GameState s = e.state();
            s.piece(0).moveTo(2);
            Position wall = s.board().positionOf(PlayerColor.RED, 3);
            placeAt(s, s.piece(4), wall); placeAt(s, s.piece(5), wall);
            e.rollDice();
            require(s.piece(0).step() == 2, "pawn did not move into block");
            require(s.currentPlayerIndex() == 1, "turn passed when no moves exist");
        });
        test("FR-30/31 own Double Block never prevents owner's movement", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            s.piece(0).moveTo(0); s.piece(1).moveTo(1); s.piece(2).moveTo(1);
            e.rollDice();
            require(s.legalMoves().stream().anyMatch(move -> move.pieceId() == 0 && move.toStep() == 2),
                    "own block is passable");
        });
        test("FR-24 opponent Double Block at entry prevents castle release", () -> {
            GameEngine e = engine(6);
            GameState s = e.state();
            Position start = s.board().positionOf(PlayerColor.RED, 0);
            placeAt(s, s.piece(4), start); placeAt(s, s.piece(5), start);
            e.rollDice();
            require(s.legalMoves().isEmpty(), "no castle piece can leave");
            require(s.piece(0).isInCastle(), "piece remains in castle");
        });
        test("FR-30 mixed owners on protected square both form blocks", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            Position safe = s.board().positionOf(PlayerColor.RED, 0);
            placeAt(s, s.piece(0), safe); placeAt(s, s.piece(1), safe);
            placeAt(s, s.piece(4), safe); placeAt(s, s.piece(5), safe);
            require(InteractionRules.blockOwners(s, safe).equals(Set.of(0, 1)), "two simultaneous owners");
            require(InteractionRules.hasOpponentDoubleBlock(s, 0, safe), "Red blocked by Green");
            require(InteractionRules.hasOpponentDoubleBlock(s, 1, safe), "Green blocked by Red");
        });
        test("FR-32 moving one of three pieces does not dissolve block", () -> {
            GameEngine e = engine(1);
            GameState s = e.state();
            for (int i=0; i<3; i++) s.piece(i).moveTo(2);
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice(); require(e.movePiece(0), "legal leave");
            require(InteractionRules.isDoubleBlock(s, s.positionOf(s.piece(1))), "two remain");
            require(events.stream().noneMatch(event -> event.blockChanges().stream()
                    .anyMatch(change -> !change.formed())), "no false dissolution event");
        });
        test("FR-31 reverse movement also stops before an opponent Double Block", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            s.piece(0).moveTo(10);
            Position wall = s.board().positionOf(PlayerColor.RED, 8);
            placeAt(s, s.piece(4), wall); placeAt(s, s.piece(5), wall);
            require(MoveValidator.furthestReachable(s, s.piece(0), 5) == 9,
                    "reverse traversal must stop before step 8");
        });
        test("FR-29 protected landing with own pieces does not invent a shield event", () -> {
            GameEngine e = engine(6);
            GameState s = e.state();
            s.piece(1).moveTo(0);
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice(); require(e.movePiece(0), "own starting square may be shared");
            require(events.size() == 1 && events.get(0).protectedLanding() == null,
                    "shield displayed only when an opponent is actually protected");
        });
        test("FR-27 invalid piece selection cannot emit a capture or block event", () -> {
            GameEngine e = engine(6);
            List<PieceInteractionEvent> events = observe(e);
            e.rollDice();
            require(!e.movePiece(4242), "invalid piece rejected");
            require(events.isEmpty(), "invalid move produced no interaction");
            require(e.state().piece(0).isInCastle(), "game state untouched");
        });
        test("Observer order is MOVE, INTERACTION, STATE", () -> {
            GameEngine e = engine(2);
            GameState s = e.state();
            s.piece(0).moveTo(0); s.piece(1).moveTo(2);
            List<String> order = new ArrayList<>();
            e.addListener(new GameListener() {
                /** Collect movement events in notification order. */
                @Override public void onPieceMoved(Move m) { order.add("MOVE"); }
                /** Collect semantic interaction events in notification order. */
                @Override public void onPieceInteraction(PieceInteractionEvent event) { order.add("INTERACTION"); }
                /** Collect committed state notifications in notification order. */
                @Override public void onStateChanged(GameState state) { order.add("STATE"); }
            });
            e.rollDice(); order.clear(); require(e.movePiece(0), "legal block-forming move");
            require(order.equals(List.of("MOVE", "INTERACTION", "STATE")), "order=" + order);
        });
        test("FR-26 exact finish is legal and a completed pawn stays in finish", () -> {
            GameEngine e = engine(1);
            GameState s = e.state();
            s.piece(0).moveTo(s.board().finishStep() - 1);
            e.rollDice(); require(e.movePiece(0), "finish succeeds");
            require(s.isFinished(s.piece(0)), "piece finished");
        });
        System.out.printf("RESULT PieceInteractionTest: %d/%d passed%n", passed, passed + failed);
        if (failed != 0) throw new AssertionError(failed + " interaction tests failed");
    }
}
