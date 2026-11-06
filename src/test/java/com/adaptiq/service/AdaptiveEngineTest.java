package com.adaptiq.service;

import static org.junit.jupiter.api.Assertions.*;

import com.adaptiq.model.Difficulty;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdaptiveEngineTest {
    private final AdaptiveEngine engine=new AdaptiveEngine();
    @Test void ninetyPercentRaisesDifficulty(){assertEquals(Difficulty.HARD,engine.determineNextDifficulty(Difficulty.MEDIUM,List.of(true,true,true,true,true)));}
    @Test void sixtyPercentMaintainsDifficulty(){assertEquals(Difficulty.MEDIUM,engine.determineNextDifficulty(Difficulty.MEDIUM,List.of(true,true,true,false,false)));}
    @Test void thirtyPercentLowersDifficulty(){assertEquals(Difficulty.EASY,engine.determineNextDifficulty(Difficulty.MEDIUM,List.of(true,false,false,false,false)));}
    @Test void boundariesAreNeverExceeded(){assertEquals(Difficulty.HARD,engine.determineNextDifficulty(Difficulty.HARD,List.of(true,true,true,true,true)));assertEquals(Difficulty.EASY,engine.determineNextDifficulty(Difficulty.EASY,List.of(false,false,false,false,false)));}
    @Test void selectionAvoidsShownQuestionAndUsesNearestDifficulty(){List<Candidate> pool=List.of(new Candidate(1,Difficulty.MEDIUM),new Candidate(2,Difficulty.HARD));assertEquals(2,engine.selectNextQuestion(Difficulty.MEDIUM,pool,Set.of(1L)).orElseThrow().questionId());}
    private record Candidate(long questionId,Difficulty difficulty) implements AdaptiveEngine.Candidate { }
}
