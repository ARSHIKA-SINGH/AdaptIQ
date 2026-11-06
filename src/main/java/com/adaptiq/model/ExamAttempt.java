package com.adaptiq.model;

import java.math.BigDecimal;
import java.time.Instant;

public final class ExamAttempt {
    private Long attemptId, studentId, examId;
    private Instant startedAt, submittedAt;
    private BigDecimal score, percentage, accuracy;
    private AttemptStatus status;
    public Long getAttemptId() { return attemptId; } public void setAttemptId(Long value) { attemptId=value; }
    public Long getStudentId() { return studentId; } public void setStudentId(Long value) { studentId=value; }
    public Long getExamId() { return examId; } public void setExamId(Long value) { examId=value; }
    public Instant getStartedAt() { return startedAt; } public void setStartedAt(Instant value) { startedAt=value; }
    public Instant getSubmittedAt() { return submittedAt; } public void setSubmittedAt(Instant value) { submittedAt=value; }
    public BigDecimal getScore() { return score; } public void setScore(BigDecimal value) { score=value; }
    public BigDecimal getPercentage() { return percentage; } public void setPercentage(BigDecimal value) { percentage=value; }
    public BigDecimal getAccuracy() { return accuracy; } public void setAccuracy(BigDecimal value) { accuracy=value; }
    public AttemptStatus getStatus() { return status; } public void setStatus(AttemptStatus value) { status=value; }
}
