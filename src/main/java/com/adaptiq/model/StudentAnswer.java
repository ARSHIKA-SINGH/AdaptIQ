package com.adaptiq.model;

import java.math.BigDecimal;
import java.time.Instant;

public final class StudentAnswer {
    private Long answerId, attemptId, questionId, selectedOptionId;
    private Boolean correct;
    private BigDecimal marksObtained;
    private Instant answeredAt;
    public Long getAnswerId() { return answerId; } public void setAnswerId(Long value) { answerId=value; }
    public Long getAttemptId() { return attemptId; } public void setAttemptId(Long value) { attemptId=value; }
    public Long getQuestionId() { return questionId; } public void setQuestionId(Long value) { questionId=value; }
    public Long getSelectedOptionId() { return selectedOptionId; } public void setSelectedOptionId(Long value) { selectedOptionId=value; }
    public Boolean getCorrect() { return correct; } public void setCorrect(Boolean value) { correct=value; }
    public BigDecimal getMarksObtained() { return marksObtained; } public void setMarksObtained(BigDecimal value) { marksObtained=value; }
    public Instant getAnsweredAt() { return answeredAt; } public void setAnsweredAt(Instant value) { answeredAt=value; }
}
