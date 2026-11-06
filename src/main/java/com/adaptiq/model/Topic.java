package com.adaptiq.model;

import java.util.Objects;

public final class Topic {
    private Long topicId;
    private Long subjectId;
    private String topicName;
    private String description;

    public Topic() { }
    public Topic(Long topicId, Long subjectId, String topicName, String description) { this.topicId = topicId; this.subjectId = subjectId; this.topicName = topicName; this.description = description; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = Objects.requireNonNull(subjectId, "subjectId"); }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = Objects.requireNonNull(topicName, "topicName"); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    @Override public String toString() { return topicName; }
}
