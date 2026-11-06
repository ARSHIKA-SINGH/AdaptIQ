package com.adaptiq.service;

import com.adaptiq.dao.ExamDAO;
import com.adaptiq.dao.ExamRepository;
import com.adaptiq.dao.ResultDAO;
import com.adaptiq.dao.ResultRepository;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.exception.ValidationException;
import com.adaptiq.model.*;
import com.adaptiq.util.SessionManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Normal-examination lifecycle; controllers never calculate results or access correct answers. */
public final class ExamService {
    private final ExamRepository exams;
    private final ResultRepository results;
    public ExamService() { this(new ExamDAO(), new ResultDAO()); }
    public ExamService(ExamRepository exams, ResultRepository results) { this.exams=Objects.requireNonNull(exams); this.results=Objects.requireNonNull(results); }
    public long createNormalExam(Exam exam, List<Long> questionIds) {
        User faculty=role(Role.FACULTY); if(exam==null || exam.getSubjectId()==null || exam.getTitle()==null || exam.getTitle().isBlank()) throw new ValidationException("Title and subject are required.");
        if(exam.getDurationMinutes()<=0) throw new ValidationException("Duration must be greater than zero."); if(questionIds==null||questionIds.isEmpty()) throw new ValidationException("Select at least one question.");
        if(exam.getTotalMarks()==null||exam.getTotalMarks().compareTo(BigDecimal.ZERO)<=0) throw new ValidationException("Total marks must be greater than zero.");
        exam.setFacultyId(faculty.getUserId()); exam.setExamMode(ExamMode.NORMAL); exam.setStatus(ExamStatus.PUBLISHED); return exams.create(exam,questionIds);
    }
    public List<Exam> availableNormalExams(){role(Role.STUDENT);return exams.findPublishedNormal();}
    public NormalExamSession startNormal(long examId){User student=role(Role.STUDENT);Exam exam=exams.findById(examId).filter(value->value.getExamMode()==ExamMode.NORMAL&&value.getStatus()==ExamStatus.PUBLISHED).orElseThrow(()->new ValidationException("This examination is not available."));ExamAttempt attempt=results.startOrResume(student.getUserId(),examId);return new NormalExamSession(exam,attempt,exams.studentQuestions(examId));}
    public void saveNormalAnswer(long attemptId,long questionId,Long optionId){User student=role(Role.STUDENT);results.saveAnswer(attemptId,student.getUserId(),questionId,optionId);}
    public EvaluationResult submitNormal(long attemptId,boolean expired){User student=role(Role.STUDENT);return results.submit(attemptId,student.getUserId(),expired);}
    private User role(Role role){User u=SessionManager.currentUser().orElseThrow(()->new AuthorizationException("Please sign in to continue."));if(u.getRole()!=role)throw new AuthorizationException("You are not authorized to access this area.");return u;}
}
