package com.adaptiq.dao;

import com.adaptiq.model.Question;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository {
    List<Question> findAll(QuestionFilter filter);
    Optional<Question> findById(long questionId);
    long create(Question question);
    void update(Question question);
    void delete(long questionId);
}
