package com.adaptiq.service;

import com.adaptiq.dao.QuestionDAO;
import com.adaptiq.dao.QuestionFilter;
import com.adaptiq.dao.QuestionRepository;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.exception.ValidationException;
import com.adaptiq.model.Question;
import com.adaptiq.model.QuestionOption;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.util.SessionManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Faculty question-bank operations with authorization and aggregate validation. */
public final class QuestionService {
    private static final Set<String> LABELS = Set.of("A", "B", "C", "D");
    private final QuestionRepository questions;
    public QuestionService() { this(new QuestionDAO()); }
    public QuestionService(QuestionRepository questions) { this.questions = Objects.requireNonNull(questions); }
    public List<Question> search(QuestionFilter filter) { requireFaculty(); return questions.findAll(filter == null ? QuestionFilter.empty() : filter); }
    public Question find(long id) { requireFaculty(); return questions.findById(id).orElseThrow(() -> new ValidationException("Question no longer exists.")); }
    public long save(Question question) {
        User faculty = requireFaculty(); validate(question);
        if (question.getQuestionId() == null) { question.setCreatedBy(faculty.getUserId()); long id = questions.create(question); question.setQuestionId(id); return id; }
        Question existing = questions.findById(question.getQuestionId()).orElseThrow(() -> new ValidationException("Question no longer exists."));
        if (!faculty.getUserId().equals(existing.getCreatedBy())) throw new AuthorizationException("You may edit only questions that you created.");
        questions.update(question); return question.getQuestionId();
    }
    public void delete(long id) {
        User faculty = requireFaculty(); Question existing = questions.findById(id).orElseThrow(() -> new ValidationException("Question no longer exists."));
        if (!faculty.getUserId().equals(existing.getCreatedBy())) throw new AuthorizationException("You may delete only questions that you created.");
        questions.delete(id);
    }
    private void validate(Question q) {
        if (q == null || q.getQuestionText() == null || q.getQuestionText().isBlank()) throw new ValidationException("Question text is required.");
        if (q.getTopicId() == null) throw new ValidationException("Select a topic.");
        if (q.getDifficulty() == null) throw new ValidationException("Select a difficulty.");
        if (q.getMarks() == null || q.getMarks().compareTo(BigDecimal.ZERO) <= 0) throw new ValidationException("Marks must be greater than zero.");
        List<QuestionOption> options = q.getOptions();
        if (options.size() != 4 || !options.stream().map(QuestionOption::getOptionLabel).collect(Collectors.toSet()).equals(LABELS)) throw new ValidationException("Exactly four options labelled A, B, C, and D are required.");
        if (options.stream().anyMatch(option -> option.getOptionText() == null || option.getOptionText().isBlank())) throw new ValidationException("Every answer option must contain text.");
        if (options.stream().filter(QuestionOption::isCorrect).count() != 1) throw new ValidationException("Exactly one correct option must be selected.");
    }
    private User requireFaculty() { User user = SessionManager.currentUser().orElseThrow(() -> new AuthorizationException("Please sign in to continue.")); if (user.getRole() != Role.FACULTY) throw new AuthorizationException("Only faculty can manage questions."); return user; }
}
