package com.adaptiq.dao;

import com.adaptiq.model.ExamAttempt;
import com.adaptiq.service.EvaluationResult;
import java.util.Map;
import java.util.Optional;

public interface ResultRepository {
    ExamAttempt startOrResume(long studentId, long examId);
    Optional<ExamAttempt> findAttempt(long attemptId, long studentId);
    void saveAnswer(long attemptId, long studentId, long questionId, Long optionId);
    EvaluationResult submit(long attemptId, long studentId, boolean expired);
}
