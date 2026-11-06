package com.adaptiq.service;

import com.adaptiq.model.Exam;
import com.adaptiq.model.ExamAttempt;
import com.adaptiq.model.ExamQuestionView;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Student-safe active examination data; remaining time is derived from the persisted start timestamp. */
public record NormalExamSession(Exam exam, ExamAttempt attempt, List<ExamQuestionView> questions) {
    public long remainingSeconds(Instant now) {
        long elapsed = Duration.between(attempt().getStartedAt(), now).getSeconds();
        return Math.max(0, (long) exam().getDurationMinutes() * 60 - elapsed);
    }
}
