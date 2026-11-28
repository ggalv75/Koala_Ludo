package Model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Cross-feature regressions for question/surprise chains on the same turn. */
class QuestionSurpriseChainTest {
    private final QuestionRepository repository;
    QuestionSurpriseChainTest() throws Exception {
        repository = QuestionRepository.fromResources();
    }

    private GameEngine engine(Map<Integer, CellType> specials, int... tickets) {
        Board base = BoardFactory.create(Difficulty.EASY, new Random(1));
        Map<Position, Cell> cells = new LinkedHashMap<>(base.cells());
        for (int step=1; step<base.homePathStart()-1; step++) {
            cells.put(base.positionOf(PlayerColor.RED,step), Cell.REGULAR);
        }
        specials.forEach((step,type) -> cells.put(base.positionOf(PlayerColor.RED,step),new Cell(type,null)));
        Map<PlayerColor,List<Position>> routes = new EnumMap<>(PlayerColor.class);
        Map<PlayerColor,Position> castles = new EnumMap<>(PlayerColor.class);
        for (PlayerColor color:PlayerColor.values()) {
            routes.put(color,base.route(color));
            castles.put(color,base.castleCorner(color));
        }
        Random draw = new Random() {
            private static final long serialVersionUID=1L;
            int index;
            @Override public int nextInt(int bound) {
                assertEquals(100,bound);
                assertTrue(index<tickets.length,"Unexpected surprise draw");
                return tickets[index++];
            }
        };
        Dice dice = new Dice() {
            @Override public DiceRoll roll() { return new DiceRoll(1); }
        };
        GameEngine engine = new GameEngine(
            new Board(base.difficulty(),cells,routes,castles,base.centerCorner()),
            List.of("Dor","Gal"),List.of(PlayerColor.RED,PlayerColor.GREEN),
            dice,Clock.systemUTC(),new SurpriseSelector(draw));
        engine.configureQuestions(new QuestionService(repository));
        engine.state().piece(0).moveTo(5);
        return engine;
    }

    private void answer(GameEngine engine, boolean correct) {
        int id=engine.state().activeQuestion().orElseThrow().id();
        for(int choice=0;choice<4;choice++) {
            if(repository.isCorrect(id,choice)==correct) {
                assertTrue(engine.submitQuestionAnswer(choice).isPresent());
                return;
            }
        }
        fail("Answer choice missing");
    }

    private List<String> events(GameEngine engine) {
        List<String> events=new ArrayList<>();
        engine.addListener(new GameListener() {
            @Override public void onPieceMoved(Move m) { events.add("move:"+m.pieceId()+":"+m.toStep()); }
            @Override public void onSurpriseDrawn(SurpriseEvent e) { events.add("surprise"); }
            @Override public void onQuestionRequested(Question q,int id) { events.add("question:"+id); }
            @Override public void onQuestionAnswered(QuestionService.AnswerResult a,int id) { events.add("answer:"+id); }
            @Override public void onPlayerFinished(PlayerFinishEvent e) { events.add("finish"); }
            @Override public void onGameOver(GameResult r) { events.add("gameOver"); }
        });
        return events;
    }

    private void start(GameEngine engine) {
        engine.rollDice();
        assertTrue(engine.movePiece(0));
    }

    @Test void surpriseMovementOpensQuestionBeforePassingTurn() {
        GameEngine e=engine(Map.of(6,CellType.SURPRISE,3,CellType.QUESTION_EASY),30);
        List<String> log=events(e);
        start(e);
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER,e.state().phase());
        assertEquals(0,e.state().currentPlayerIndex());
        assertEquals(List.of("move:0:6","surprise","move:0:3","question:0"),log);
        answer(e,true);
        assertEquals(4,e.state().piece(0).step());
        assertEquals(1,e.state().currentPlayerIndex());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL,e.state().phase());
    }

    @Test void correctAnswerTriggersCastleSurprise() {
        GameEngine e=engine(Map.of(6,CellType.QUESTION_EASY,7,CellType.SURPRISE),20);
        List<String> log=events(e);
        start(e); answer(e,true);
        assertTrue(e.state().piece(0).isInCastle());
        assertEquals(1,e.state().currentPlayerIndex());
        assertEquals(List.of("move:0:6","question:0","answer:0","move:0:7","surprise","move:0:-1"),log);
    }

    @Test void wrongAnswerCanTriggerSpecialRollSurprise() {
        GameEngine e=engine(Map.of(6,CellType.QUESTION_EASY,5,CellType.SURPRISE),90);
        start(e); answer(e,false);
        assertTrue(e.state().specialRollPending());
        assertEquals(MultiplierDie.ULTRA_BOOST,e.state().nextThrowMultiplier());
        assertEquals(0,e.state().currentPlayerIndex());
        assertEquals(GameState.Phase.WAITING_FOR_ROLL,e.state().phase());
        assertTrue(e.state().activeQuestion().isEmpty());
    }

    @Test void repeatedQuestionSurpriseQuestionChainPassesTurnOnlyAtEnd() {
        GameEngine e=engine(Map.of(6,CellType.QUESTION_EASY,7,CellType.SURPRISE,4,CellType.QUESTION_EASY),30);
        start(e);answer(e,true);
        assertEquals(4,e.state().piece(0).step());
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER,e.state().phase());
        assertEquals(0,e.state().currentPlayerIndex());
        answer(e,true);
        assertEquals(5,e.state().piece(0).step());
        assertEquals(1,e.state().currentPlayerIndex());
        assertTrue(e.state().pendingQuestionResult().isEmpty());
    }

    @Test void opponentMovedOntoQuestionUsesThatPieceAndRetainsActiveTurnUntilAnswer() {
        GameEngine e=engine(Map.of(6,CellType.SURPRISE,9,CellType.QUESTION_EASY),10);
        Position start=e.state().board().positionOf(PlayerColor.RED,10);
        int step=e.state().board().route(PlayerColor.GREEN).indexOf(start);
        assertTrue(step>0);
        e.state().piece(4).moveTo(step);
        start(e);
        assertEquals(GameState.Phase.WAITING_FOR_SURPRISE_CHOICE,e.state().phase());
        assertTrue(e.selectSurpriseOpponentPiece(4));
        assertEquals(GameState.Phase.WAITING_FOR_QUESTION_ANSWER,e.state().phase());
        assertEquals(4,e.state().questionPieceId().orElseThrow());
        assertEquals(0,e.state().currentPlayerIndex());
        answer(e,true);
        assertEquals(step,e.state().piece(4).step());
        assertEquals(6,e.state().piece(0).step());
        assertEquals(1,e.state().currentPlayerIndex());
    }

    @Test void invalidActionsDoNotConsumeChainedQuestionOrRoll() {
        GameEngine e=engine(Map.of(6,CellType.SURPRISE,3,CellType.QUESTION_EASY),30);
        start(e);
        int id=e.state().activeQuestion().orElseThrow().id();
        e.rollDice(); assertFalse(e.movePiece(0));
        assertTrue(e.submitQuestionAnswer(-1).isEmpty());
        assertEquals(id,e.state().activeQuestion().orElseThrow().id());
        assertEquals(3,e.state().piece(0).step());
        assertEquals(0,e.state().currentPlayerIndex());
        answer(e,true);
        assertTrue(e.submitQuestionAnswer(0).isEmpty());
        assertEquals(4,e.state().piece(0).step());
    }

    @Test void questionThenDirectFinishRanksOnceBeforeGameOver() {
        GameEngine e=engine(Map.of(6,CellType.QUESTION_EASY,7,CellType.SURPRISE),0);
        for(int id=1;id<4;id++) e.state().piece(id).moveTo(e.state().board().finishStep());
        List<String> log=events(e);
        start(e);answer(e,true);
        assertEquals(GameState.Phase.GAME_OVER,e.state().phase());
        assertEquals(1,e.state().players().get(0).rank());
        assertEquals(1,Collections.frequency(log,"finish"));
        assertEquals(1,Collections.frequency(log,"gameOver"));
        assertTrue(log.indexOf("finish")<log.indexOf("gameOver"));
        assertTrue(e.state().activeQuestion().isEmpty());
    }

    @Test void mediumBoardContainsAllSpecialSquaresWithoutReplacingFixedCells() {
        Map<Position,Cell> fixed=new MediumBoardLayout().fixedCells();
        for(int seed=0;seed<100;seed++) {
            Board b=BoardFactory.create(Difficulty.MEDIUM,new Random(seed));
            assertEquals(8,b.count(CellType.SAFE));
            assertEquals(2,b.count(CellType.SURPRISE));
            assertEquals(2,b.count(CellType.QUESTION_EASY));
            assertEquals(2,b.count(CellType.QUESTION_MEDIUM));
            assertEquals(2,b.count(CellType.QUESTION_HARD));
            for(var entry:fixed.entrySet()) {
                if(entry.getValue().type()!=CellType.REGULAR || entry.getValue().color()!=null)
                    assertEquals(entry.getValue(),b.cellAt(entry.getKey()));
            }
        }
    }
}
