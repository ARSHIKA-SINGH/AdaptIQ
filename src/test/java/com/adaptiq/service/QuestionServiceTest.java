package com.adaptiq.service;

import static org.junit.jupiter.api.Assertions.*;

import com.adaptiq.dao.QuestionFilter;
import com.adaptiq.dao.QuestionRepository;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.exception.ValidationException;
import com.adaptiq.model.Difficulty;
import com.adaptiq.model.Question;
import com.adaptiq.model.QuestionOption;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.model.UserStatus;
import com.adaptiq.util.SessionManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QuestionServiceTest {
    @AfterEach void clearSession() { SessionManager.clear(); }

    @Test void facultyCanCreateAValidFourOptionQuestion() {
        SessionManager.start(user(Role.FACULTY));
        InMemoryQuestions repository = new InMemoryQuestions();
        Question question = validQuestion();

        long id = new QuestionService(repository).save(question);

        assertEquals(1L, id);
        assertEquals(1L, question.getCreatedBy());
        assertEquals(1, repository.created);
    }

    @Test void questionNeedsExactlyOneCorrectOption() {
        SessionManager.start(user(Role.FACULTY));
        Question question = validQuestion();
        question.setOptions(List.of(option("A", true), option("B", true), option("C", false), option("D", false)));

        assertThrows(ValidationException.class, () -> new QuestionService(new InMemoryQuestions()).save(question));
    }

    @Test void studentCannotManageQuestions() {
        SessionManager.start(user(Role.STUDENT));
        assertThrows(AuthorizationException.class, () -> new QuestionService(new InMemoryQuestions()).search(QuestionFilter.empty()));
    }

    private static Question validQuestion() {
        Question question = new Question(); question.setTopicId(5L); question.setQuestionText("What does JVM stand for?"); question.setDifficulty(Difficulty.EASY); question.setMarks(BigDecimal.ONE);
        question.setOptions(List.of(option("A", false), option("B", true), option("C", false), option("D", false))); return question;
    }
    private static QuestionOption option(String label, boolean correct) { return new QuestionOption(null, null, label, "Option " + label, correct); }
    private static User user(Role role) { return new User(1L, "Faculty User", "faculty@adaptiq.test", "hash", role, UserStatus.ACTIVE, null); }
    private static final class InMemoryQuestions implements QuestionRepository {
        int created;
        public List<Question> findAll(QuestionFilter filter) { return List.of(); }
        public Optional<Question> findById(long questionId) { return Optional.empty(); }
        public long create(Question question) { created++; return 1L; }
        public void update(Question question) { }
        public void delete(long questionId) { }
    }
}
