package com.adaptiq.dao;

import com.adaptiq.model.dashboard.AdminDashboardData;
import com.adaptiq.model.dashboard.FacultyDashboardData;
import com.adaptiq.model.dashboard.StudentDashboardData;

/** Read-model contract for role dashboards. */
public interface DashboardRepository {
    StudentDashboardData loadStudentDashboard(long studentId);
    FacultyDashboardData loadFacultyDashboard(long facultyId);
    AdminDashboardData loadAdminDashboard();
}
