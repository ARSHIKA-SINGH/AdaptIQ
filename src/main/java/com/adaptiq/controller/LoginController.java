package com.adaptiq.controller;

import com.adaptiq.service.AuthService;
import com.adaptiq.util.SceneNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Thin JavaFX controller: sign-in logic remains in AuthService. */
public final class LoginController {
    private final AuthService authService = new AuthService();
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    @FXML
    private void handleLogin() {
        setBusy(true);
        String email = emailField.getText();
        String password = passwordField.getText();
        Task<String> task = new Task<>() {
            @Override protected String call() {
                return authService.authenticate(email, password).getFullName();
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false);
            switch (authServiceRole()) {
                case STUDENT -> SceneNavigator.show("student_dashboard");
                case FACULTY -> SceneNavigator.show("faculty_dashboard");
                case ADMIN -> SceneNavigator.show("admin_dashboard");
            }
        });
        task.setOnFailed(event -> {
            setBusy(false);
            statusLabel.setText(messageFor(task.getException()));
        });
        Thread thread = new Thread(task, "adaptiq-login");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML private void showRegistration() { SceneNavigator.show("register"); }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        statusLabel.setText(busy ? "Signing in…" : "");
    }

    private String messageFor(Throwable error) {
        return error == null || error.getMessage() == null ? "Unable to sign in. Please try again." : error.getMessage();
    }

    private com.adaptiq.model.Role authServiceRole() {
        return com.adaptiq.util.SessionManager.currentUser()
                .orElseThrow(() -> new IllegalStateException("Sign-in session was not created."))
                .getRole();
    }
}
