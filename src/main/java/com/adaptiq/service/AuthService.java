package com.adaptiq.service;

import com.adaptiq.dao.ActivityLogDAO;
import com.adaptiq.dao.ActivityLogRepository;
import com.adaptiq.dao.UserAccountRepository;
import com.adaptiq.dao.UserDAO;
import com.adaptiq.exception.AuthenticationException;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.exception.ValidationException;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.model.UserStatus;
import com.adaptiq.util.PasswordUtil;
import com.adaptiq.util.SessionManager;
import com.adaptiq.util.ValidationUtil;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Authentication and self-service student registration business rules. */
public final class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private final UserAccountRepository users;
    private final ActivityLogRepository activityLogs;

    public AuthService() { this(new UserDAO(), new ActivityLogDAO()); }

    public AuthService(UserAccountRepository users, ActivityLogRepository activityLogs) {
        this.users = Objects.requireNonNull(users, "users");
        this.activityLogs = Objects.requireNonNull(activityLogs, "activityLogs");
    }

    public User authenticate(String email, String password) {
        String normalizedEmail = ValidationUtil.normalizeAndValidateEmail(email);
        if (password == null || password.isEmpty()) {
            throw new AuthenticationException("Email or password is incorrect.");
        }
        User user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationException("Email or password is incorrect."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("This account is not active. Contact an administrator.");
        }
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            safeLog(user.getUserId(), "FAILED_SIGN_IN");
            throw new AuthenticationException("Email or password is incorrect.");
        }
        SessionManager.start(user);
        safeLog(user.getUserId(), "SIGNED_IN");
        return user;
    }

    /** Public registration intentionally creates STUDENT accounts only. */
    public User registerStudent(RegistrationRequest request) {
        Objects.requireNonNull(request, "request");
        String fullName = ValidationUtil.validateName(request.fullName());
        String email = ValidationUtil.normalizeAndValidateEmail(request.email());
        ValidationUtil.validatePassword(request.password());
        if (!Objects.equals(request.password(), request.confirmPassword())) {
            throw new ValidationException("Password confirmation does not match.");
        }
        if (users.emailExists(email)) {
            throw new ValidationException("An account with that email already exists.");
        }
        User student = new User(null, fullName, email, PasswordUtil.hash(request.password()), Role.STUDENT, UserStatus.ACTIVE, null);
        try {
            student.setUserId(users.create(student));
        } catch (DatabaseException exception) {
            if (exception.getMessage().contains("already exists")) {
                throw new ValidationException("An account with that email already exists.");
            }
            throw exception;
        }
        safeLog(student.getUserId(), "STUDENT_REGISTERED");
        return student;
    }

    public void logout() {
        SessionManager.currentUser().ifPresent(user -> safeLog(user.getUserId(), "SIGNED_OUT"));
        SessionManager.clear();
    }

    private void safeLog(Long userId, String activity) {
        try {
            activityLogs.record(userId, activity);
        } catch (DatabaseException exception) {
            // Authentication must not be silently undone if non-critical audit logging fails.
            LOGGER.warn("Audit log write failed for {}: {}", activity, exception.getMessage());
        }
    }
}
