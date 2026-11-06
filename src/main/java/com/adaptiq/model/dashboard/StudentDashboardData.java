package com.adaptiq.model.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record StudentDashboardData(long totalAttempts, BigDecimal averagePercentage, BigDecimal overallAccuracy,
                                   BigDecimal highestScore, List<AttemptSummary> recentAttempts,
                                   List<TopicInsight> weakestTopics, List<TopicInsight> strongestTopics,
                                   List<RecommendationSummary> recommendations) { }
