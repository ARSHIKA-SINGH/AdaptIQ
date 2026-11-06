package com.adaptiq.service;
import java.math.BigDecimal; import java.util.List;
public record AnalyticsSnapshot(long testsAttempted,BigDecimal averageScore,BigDecimal highestScore,BigDecimal lowestScore,BigDecimal overallAccuracy,List<NamedAccuracy> topicAccuracy,List<NamedAccuracy> difficultyAccuracy,List<TrendPoint> performanceTrend){public record NamedAccuracy(String name,BigDecimal accuracy){} public record TrendPoint(String dateLabel,BigDecimal percentage){}}
