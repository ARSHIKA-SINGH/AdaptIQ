package com.adaptiq.service;

import static org.junit.jupiter.api.Assertions.*;

import com.adaptiq.dao.DashboardRepository;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.model.UserStatus;
import com.adaptiq.model.dashboard.AdminDashboardData;
import com.adaptiq.model.dashboard.FacultyDashboardData;
import com.adaptiq.model.dashboard.StudentDashboardData;
import com.adaptiq.util.SessionManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
    @AfterEach void clearSession() { SessionManager.clear(); }

    @Test void studentCanLoadOnlyOwnDashboardProjection() {
        SessionManager.start(user(Role.STUDENT));
        DashboardService service = new DashboardService(new StubDashboardRepository());

        StudentDashboardData data = service.studentDashboard();

        assertEquals(2, data.totalAttempts());
        assertThrows(AuthorizationException.class, service::adminDashboard);
    }

    @Test void facultyCannotRequestStudentProjection() {
        SessionManager.start(user(Role.FACULTY));
        DashboardService service = new DashboardService(new StubDashboardRepository());

        assertThrows(AuthorizationException.class, service::studentDashboard);
    }

    private static User user(Role role) {
        return new User(7L, "Test User", "user@adaptiq.test", "hash", role, UserStatus.ACTIVE, null);
    }

    private static final class StubDashboardRepository implements DashboardRepository {
        public StudentDashboardData loadStudentDashboard(long id) {
            return new StudentDashboardData(2, BigDecimal.valueOf(75), BigDecimal.valueOf(80), BigDecimal.valueOf(18), List.of(), List.of(), List.of(), List.of());
        }
        public FacultyDashboardData loadFacultyDashboard(long id) { return new FacultyDashboardData(1, 1, 2, BigDecimal.valueOf(75)); }
        public AdminDashboardData loadAdminDashboard() { return new AdminDashboardData(2, 1, 1, 10); }
    }
}
