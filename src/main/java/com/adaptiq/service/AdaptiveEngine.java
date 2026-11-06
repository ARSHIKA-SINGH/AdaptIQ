package com.adaptiq.service;

import com.adaptiq.model.Difficulty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Pure, deterministic rule engine. Persistence and JavaFX are deliberately kept outside this class. */
public final class AdaptiveEngine {
    public static final int RECENT_WINDOW = 5;
    public BigDecimal calculateRecentAccuracy(List<Boolean> answers) {
        if (answers == null || answers.isEmpty()) return BigDecimal.ZERO;
        List<Boolean> recent = answers.subList(Math.max(0, answers.size() - RECENT_WINDOW), answers.size());
        long correct = recent.stream().filter(Boolean.TRUE::equals).count();
        return BigDecimal.valueOf(correct).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(recent.size()), 2, RoundingMode.HALF_UP);
    }
    public Difficulty determineNextDifficulty(Difficulty current, List<Boolean> answers) {
        if (current == null) throw new IllegalArgumentException("Current difficulty is required.");
        BigDecimal accuracy = calculateRecentAccuracy(answers);
        if (accuracy.compareTo(BigDecimal.valueOf(80)) >= 0) return shift(current, 1);
        if (accuracy.compareTo(BigDecimal.valueOf(50)) < 0) return shift(current, -1);
        return current;
    }
    public <T extends Candidate> Optional<T> selectNextQuestion(Difficulty target, List<T> pool, Set<Long> shownQuestionIds) {
        if (target == null || pool == null) return Optional.empty();
        return pool.stream().filter(question -> !shownQuestionIds.contains(question.questionId()))
                .min(Comparator.<T>comparingInt(question -> distance(target, question.difficulty()))
                        .thenComparingLong(Candidate::questionId));
    }
    public Difficulty getCurrentDifficulty(List<Decision> history) { return history == null || history.isEmpty() ? Difficulty.MEDIUM : history.get(history.size()-1).selectedDifficulty(); }
    private Difficulty shift(Difficulty value, int amount) { int index=Math.max(0,Math.min(Difficulty.values().length-1,value.ordinal()+amount));return Difficulty.values()[index]; }
    private int distance(Difficulty a,Difficulty b){return Math.abs(a.ordinal()-b.ordinal());}
    public interface Candidate { long questionId(); Difficulty difficulty(); }
    public record Decision(Difficulty previousDifficulty, Difficulty selectedDifficulty, BigDecimal accuracyAtDecision) { }
}
