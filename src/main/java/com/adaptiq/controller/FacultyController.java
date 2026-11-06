package com.adaptiq.controller;

import com.adaptiq.model.dashboard.FacultyDashboardData;
import com.adaptiq.service.AuthService;
import com.adaptiq.service.DashboardService;
import com.adaptiq.service.PdfReportService;
import com.adaptiq.util.SceneNavigator;
import com.adaptiq.util.SessionManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;

public final class FacultyController {
    private final DashboardService dashboardService = new DashboardService();
    private final AuthService authService = new AuthService();
    @FXML private Label userNameLabel, examsLabel, publishedLabel, attemptsLabel, averageLabel, statusLabel;
    @FXML private Button refreshButton;
    @FXML private void initialize() { userNameLabel.setText(SessionManager.currentUser().map(user -> user.getFullName()).orElse("Faculty")); refresh(); }
    @FXML private void refresh() {
        refreshButton.setDisable(true); statusLabel.setText("Loading examination statistics…");
        Task<FacultyDashboardData> task = new Task<>() { @Override protected FacultyDashboardData call() { return dashboardService.facultyDashboard(); } };
        task.setOnSucceeded(event -> { FacultyDashboardData data = task.getValue(); examsLabel.setText(Long.toString(data.totalExams())); publishedLabel.setText(Long.toString(data.publishedExams())); attemptsLabel.setText(Long.toString(data.totalAttempts())); averageLabel.setText(percent(data.averagePercentage())); refreshButton.setDisable(false); statusLabel.setText(""); });
        task.setOnFailed(event -> { refreshButton.setDisable(false); statusLabel.setText(messageFor(task.getException())); });
        Thread thread = new Thread(task, "adaptiq-faculty-dashboard"); thread.setDaemon(true); thread.start();
    }
    @FXML private void logout() { authService.logout(); SceneNavigator.show("login"); }
    @FXML private void showQuestionBank() { SceneNavigator.show("question_bank"); }
    @FXML private void showExamBuilder() { SceneNavigator.show("exam_builder"); }
    @FXML private void exportPdf() { FileChooser chooser=new FileChooser();chooser.setInitialFileName("faculty-examination-report.pdf");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF document","*.pdf"));File file=chooser.showSaveDialog(statusLabel.getScene().getWindow());if(file==null)return;Task<Void> task=new Task<>(){@Override protected Void call(){new PdfReportService().generateFacultyReport(file.toPath());return null;}};task.setOnSucceeded(event->statusLabel.setText("Report saved to "+file.getName()));task.setOnFailed(event->statusLabel.setText(task.getException()==null?"Could not create report.":task.getException().getMessage()));Thread thread=new Thread(task,"adaptiq-faculty-report");thread.setDaemon(true);thread.start(); }
    private String percent(BigDecimal value) { return (value == null ? "0" : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()) + "%"; }
    private String messageFor(Throwable error) { return error == null || error.getMessage() == null ? "Unable to load dashboard data." : error.getMessage(); }
}
