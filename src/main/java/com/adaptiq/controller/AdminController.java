package com.adaptiq.controller;

import com.adaptiq.model.dashboard.AdminDashboardData;
import com.adaptiq.service.AuthService;
import com.adaptiq.service.DashboardService;
import com.adaptiq.util.SceneNavigator;
import com.adaptiq.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public final class AdminController {
    private final DashboardService dashboardService = new DashboardService();
    private final AuthService authService = new AuthService();
    @FXML private Label userNameLabel, studentsLabel, facultyLabel, examsLabel, questionsLabel, statusLabel;
    @FXML private Button refreshButton;
    @FXML private void initialize() { userNameLabel.setText(SessionManager.currentUser().map(user -> user.getFullName()).orElse("Administrator")); refresh(); }
    @FXML private void refresh() {
        refreshButton.setDisable(true); statusLabel.setText("Loading system statistics…");
        Task<AdminDashboardData> task = new Task<>() { @Override protected AdminDashboardData call() { return dashboardService.adminDashboard(); } };
        task.setOnSucceeded(event -> { AdminDashboardData data = task.getValue(); studentsLabel.setText(Long.toString(data.totalStudents())); facultyLabel.setText(Long.toString(data.totalFaculty())); examsLabel.setText(Long.toString(data.totalExams())); questionsLabel.setText(Long.toString(data.totalQuestions())); refreshButton.setDisable(false); statusLabel.setText(""); });
        task.setOnFailed(event -> { refreshButton.setDisable(false); statusLabel.setText(messageFor(task.getException())); });
        Thread thread = new Thread(task, "adaptiq-admin-dashboard"); thread.setDaemon(true); thread.start();
    }
    @FXML private void logout() { authService.logout(); SceneNavigator.show("login"); }
    @FXML private void showCatalog() { SceneNavigator.show("catalog_management"); }
    private String messageFor(Throwable error) { return error == null || error.getMessage() == null ? "Unable to load dashboard data." : error.getMessage(); }
}
