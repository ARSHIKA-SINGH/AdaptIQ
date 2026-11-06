package com.adaptiq.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Exam {
    private Long examId, subjectId, facultyId;
    private String title, description;
    private int durationMinutes;
    private BigDecimal totalMarks;
    private ExamMode examMode;
    private ExamStatus status;
    private Instant createdAt;
    private List<Question> questions = new ArrayList<>();
    public Long getExamId() { return examId; } public void setExamId(Long value) { examId=value; }
    public Long getSubjectId() { return subjectId; } public void setSubjectId(Long value) { subjectId=value; }
    public Long getFacultyId() { return facultyId; } public void setFacultyId(Long value) { facultyId=value; }
    public String getTitle() { return title; } public void setTitle(String value) { title=value; }
    public String getDescription() { return description; } public void setDescription(String value) { description=value; }
    public int getDurationMinutes() { return durationMinutes; } public void setDurationMinutes(int value) { durationMinutes=value; }
    public BigDecimal getTotalMarks() { return totalMarks; } public void setTotalMarks(BigDecimal value) { totalMarks=value; }
    public ExamMode getExamMode() { return examMode; } public void setExamMode(ExamMode value) { examMode=value; }
    public ExamStatus getStatus() { return status; } public void setStatus(ExamStatus value) { status=value; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant value) { createdAt=value; }
    public List<Question> getQuestions() { return List.copyOf(questions); } public void setQuestions(List<Question> value) { questions=new ArrayList<>(value); }
    @Override public String toString() { return title; }
}
