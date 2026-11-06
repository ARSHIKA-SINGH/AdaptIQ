package com.adaptiq.dao;

import com.adaptiq.model.Exam;
import com.adaptiq.model.ExamQuestionView;
import java.util.List;
import java.util.Optional;

public interface ExamRepository {
    long create(Exam exam, List<Long> questionIds);
    List<Exam> findPublishedNormal();
    Optional<Exam> findById(long examId);
    List<ExamQuestionView> studentQuestions(long examId);
}
