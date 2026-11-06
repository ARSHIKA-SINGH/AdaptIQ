package com.adaptiq.model.dashboard;

import com.adaptiq.model.Difficulty;

public record RecommendationSummary(String topicName, String recommendationText, Difficulty difficulty) { }
