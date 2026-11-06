package com.adaptiq.controller;

import com.adaptiq.service.AuthService;
import com.adaptiq.service.RegistrationRequest;
import com.adaptiq.util.SceneNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Student registration screen; faculty and administrator accounts are never self-provisioned. */
public final class RegisterController {
    private final AuthService authService = new AuthService();
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;

    @FXML
    private void handleRegistration() {
        setBusy(true);
        RegistrationRequest request = new RegistrationRequest(fullNameField.getText(), emailField.getText(),
                passwordField.getText(), confirmPasswordField.getText());
        Task<String> task = new Task<>() {
            @Override protected String call() { return authService.registerStudent(request).getFullName(); }
        };
        task.setOnSucceeded(event -> {
            setBusy(false);
            statusLabel.setText("Account created for " + task.getValue() + ". You can now sign in.");
            passwordField.clear();
            confirmPasswordField.clear();
        });
        task.setOnFailed(event -> { setBusy(false); statusLabel.setText(messageFor(task.getException())); });
        Thread thread = new Thread(task, "adaptiq-registration");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML private void showLogin() { SceneNavigator.show("login"); }

    private void setBusy(boolean busy) {
        registerButton.setDisable(busy);
        statusLabel.setText(busy ? "Creating your account…" : "");
    }

    private String messageFor(Throwable error) {
        return error == null || error.getMessage() == null ? "Unable to create the account. Please try again." : error.getMessage();
    }
}
