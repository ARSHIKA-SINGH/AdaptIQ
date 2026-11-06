package com.adaptiq.service;

import static org.junit.jupiter.api.Assertions.*;

import com.adaptiq.dao.ActivityLogRepository;
import com.adaptiq.dao.UserAccountRepository;
import com.adaptiq.exception.AuthenticationException;
import com.adaptiq.exception.ValidationException;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.model.UserStatus;
import com.adaptiq.util.PasswordUtil;
import com.adaptiq.util.SessionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
    @AfterEach void clearSession() { SessionManager.clear(); }

    @Test void validLoginStartsSession() {
        InMemoryUsers users = new InMemoryUsers();
        users.create(user("student@adaptiq.test", "Password123"));
        AuthService service = new AuthService(users, (id, activity) -> { });

        User authenticated = service.authenticate("STUDENT@adaptiq.test", "Password123");

        assertEquals(Role.STUDENT, authenticated.getRole());
        assertTrue(SessionManager.currentUser().isPresent());
    }

    @Test void invalidLoginIsRejected() {
        InMemoryUsers users = new InMemoryUsers();
        users.create(user("student@adaptiq.test", "Password123"));
        AuthService service = new AuthService(users, (id, activity) -> { });

        assertThrows(AuthenticationException.class, () -> service.authenticate("student@adaptiq.test", "wrong"));
    }

    @Test void duplicateStudentRegistrationIsRejected() {
        InMemoryUsers users = new InMemoryUsers();
        users.create(user("student@adaptiq.test", "Password123"));
        AuthService service = new AuthService(users, (id, activity) -> { });

        assertThrows(ValidationException.class, () -> service.registerStudent(
                new RegistrationRequest("Ada Student", "student@adaptiq.test", "Password123", "Password123")));
    }

    @Test void passwordHashVerifiesWithoutStoringPlainText() {
        String hash = PasswordUtil.hash("Password123");
        assertNotEquals("Password123", hash);
        assertTrue(PasswordUtil.matches("Password123", hash));
        assertFalse(PasswordUtil.matches("not-the-password", hash));
    }

    private static User user(String email, String password) {
        return new User(null, "Ada Student", email, PasswordUtil.hash(password), Role.STUDENT, UserStatus.ACTIVE, null);
    }

    private static final class InMemoryUsers implements UserAccountRepository {
        private final Map<String, User> users = new HashMap<>();
        private long nextId = 1;
        public Optional<User> findByEmail(String email) { return Optional.ofNullable(users.get(email)); }
        public boolean emailExists(String email) { return users.containsKey(email); }
        public long create(User user) { long id = nextId++; user.setUserId(id); users.put(user.getEmail(), user); return id; }
    }
}
