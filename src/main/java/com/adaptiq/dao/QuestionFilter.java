package com.adaptiq.dao;

import com.adaptiq.model.Difficulty;

/** Optional filters for question-bank retrieval. */
public record QuestionFilter(Long subjectId, Long topicId, Difficulty difficulty, String searchText) {
    public static QuestionFilter empty() { return new QuestionFilter(null, null, null, null); }
}
