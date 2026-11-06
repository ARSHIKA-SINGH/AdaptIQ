package com.adaptiq.service;

import com.adaptiq.model.Exam;
import com.adaptiq.model.ExamAttempt;
import com.adaptiq.model.ExamQuestionView;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record AdaptiveExamSession(Exam exam, ExamAttempt attempt, Optional<ExamQuestionView> currentQuestion) {
    public long remainingSeconds(Instant now) { return Math.max(0, (long) exam().getDurationMinutes() * 60 - Duration.between(attempt().getStartedAt(), now).getSeconds()); }
}
