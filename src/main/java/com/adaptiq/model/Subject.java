package com.adaptiq.model;

import java.util.Objects;

public final class Subject {
    private Long subjectId;
    private String subjectName;
    private String description;

    public Subject() { }
    public Subject(Long subjectId, String subjectName, String description) { this.subjectId = subjectId; this.subjectName = subjectName; this.description = description; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = Objects.requireNonNull(subjectName, "subjectName"); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    @Override public String toString() { return subjectName; }
}
