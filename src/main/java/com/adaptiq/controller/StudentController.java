package com.adaptiq.controller;

import com.adaptiq.model.dashboard.AttemptSummary;
import com.adaptiq.model.dashboard.RecommendationSummary;
import com.adaptiq.model.dashboard.StudentDashboardData;
import com.adaptiq.model.dashboard.TopicInsight;
import com.adaptiq.service.AuthService;
import com.adaptiq.service.DashboardService;
import com.adaptiq.util.SceneNavigator;
import com.adaptiq.util.SessionManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

/** Student dashboard presentation only; its read model is loaded through DashboardService. */
public final class StudentController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM uuuu").withZone(ZoneId.systemDefault());
    private final DashboardService dashboardService = new DashboardService();
    private final AuthService authService = new AuthService();
    @FXML private Label userNameLabel, attemptsLabel, averageLabel, accuracyLabel, highScoreLabel, statusLabel;
    @FXML private ListView<String> recentAttemptsList, weakTopicsList, strongTopicsList, recommendationsList;
    @FXML private Button refreshButton;

    @FXML private void initialize() {
        userNameLabel.setText(SessionManager.currentUser().map(user -> user.getFullName()).orElse("Student"));
        refresh();
    }

    @FXML private void refresh() {
        refreshButton.setDisable(true);
        statusLabel.setText("Loading your dashboard…");
        Task<StudentDashboardData> task = new Task<>() { @Override protected StudentDashboardData call() { return dashboardService.studentDashboard(); } };
        task.setOnSucceeded(event -> { populate(task.getValue()); refreshButton.setDisable(false); statusLabel.setText(""); });
        task.setOnFailed(event -> { refreshButton.setDisable(false); statusLabel.setText(messageFor(task.getException())); });
        start(task, "adaptiq-student-dashboard");
    }

    @FXML private void logout() { authService.logout(); SceneNavigator.show("login"); }
    @FXML private void showAvailableExams() { SceneNavigator.show("available_exams"); }
    @FXML private void showAnalytics() { SceneNavigator.show("analytics"); }
    @FXML private void showRecommendations() { SceneNavigator.show("recommendations"); }

    private void populate(StudentDashboardData data) {
        attemptsLabel.setText(Long.toString(data.totalAttempts()));
        averageLabel.setText(percent(data.averagePercentage()));
        accuracyLabel.setText(percent(data.overallAccuracy()));
        highScoreLabel.setText(decimal(data.highestScore()));
        recentAttemptsList.setItems(FXCollections.observableArrayList(data.recentAttempts().stream().map(this::attemptText).toList()));
        weakTopicsList.setItems(FXCollections.observableArrayList(data.weakestTopics().stream().map(this::topicText).toList()));
        strongTopicsList.setItems(FXCollections.observableArrayList(data.strongestTopics().stream().map(this::topicText).toList()));
        recommendationsList.setItems(FXCollections.observableArrayList(data.recommendations().stream().map(this::recommendationText).toList()));
    }
    private String attemptText(AttemptSummary item) { return item.examTitle() + " • " + percent(item.percentage()) + " • " + DATE_FORMAT.format(item.submittedAt()); }
    private String topicText(TopicInsight item) { return item.topicName() + " • " + percent(item.accuracy()); }
    private String recommendationText(RecommendationSummary item) { return item.topicName() + " — " + item.recommendationText() + " (" + item.difficulty() + ")"; }
    private String percent(BigDecimal value) { return decimal(value) + "%"; }
    private String decimal(BigDecimal value) { return value == null ? "0" : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
    private String messageFor(Throwable error) { return error == null || error.getMessage() == null ? "Unable to load dashboard data." : error.getMessage(); }
    private void start(Task<?> task, String name) { Thread thread = new Thread(task, name); thread.setDaemon(true); thread.start(); }
}
