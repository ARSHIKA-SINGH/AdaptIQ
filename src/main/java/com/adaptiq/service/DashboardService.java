package com.adaptiq.service;

import com.adaptiq.dao.DashboardDAO;
import com.adaptiq.dao.DashboardRepository;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.model.dashboard.AdminDashboardData;
import com.adaptiq.model.dashboard.FacultyDashboardData;
import com.adaptiq.model.dashboard.StudentDashboardData;
import com.adaptiq.util.SessionManager;
import java.util.Objects;

/** Service-level authorization prevents data access based solely on hidden navigation buttons. */
public final class DashboardService {
    private final DashboardRepository dashboardRepository;

    public DashboardService() { this(new DashboardDAO()); }
    public DashboardService(DashboardRepository dashboardRepository) { this.dashboardRepository = Objects.requireNonNull(dashboardRepository); }

    public StudentDashboardData studentDashboard() { return dashboardRepository.loadStudentDashboard(requireRole(Role.STUDENT).getUserId()); }
    public FacultyDashboardData facultyDashboard() { return dashboardRepository.loadFacultyDashboard(requireRole(Role.FACULTY).getUserId()); }
    public AdminDashboardData adminDashboard() { requireRole(Role.ADMIN); return dashboardRepository.loadAdminDashboard(); }

    private User requireRole(Role role) {
        User user = SessionManager.currentUser().orElseThrow(() -> new AuthorizationException("Please sign in to continue."));
        if (user.getRole() != role) throw new AuthorizationException("You are not authorized to access this area.");
        return user;
    }
}
