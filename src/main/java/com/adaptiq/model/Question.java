package com.adaptiq.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** MCQ question aggregate. Options are retained as part of the aggregate for transactional writes. */
public final class Question {
    private Long questionId;
    private Long topicId;
    private String topicName;
    private String questionText;
    private Difficulty difficulty;
    private BigDecimal marks;
    private String explanation;
    private Long createdBy;
    private Instant createdAt;
    private List<QuestionOption> options = new ArrayList<>();

    public Question() { }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = Objects.requireNonNull(topicId, "topicId"); }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = Objects.requireNonNull(questionText, "questionText"); }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = Objects.requireNonNull(difficulty, "difficulty"); }
    public BigDecimal getMarks() { return marks; }
    public void setMarks(BigDecimal marks) { this.marks = Objects.requireNonNull(marks, "marks"); }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<QuestionOption> getOptions() { return List.copyOf(options); }
    public void setOptions(List<QuestionOption> options) { this.options = new ArrayList<>(Objects.requireNonNull(options, "options")); }
    @Override public String toString() { return questionText == null ? "Question" : questionText.length() > 90 ? questionText.substring(0, 87) + "..." : questionText; }
}
