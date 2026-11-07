
package com.adaptiq.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.adaptiq.model.ExamQuestionView;
import com.adaptiq.service.EvaluationResult;
import com.adaptiq.service.ExamService;
import com.adaptiq.service.NormalExamSession;
import com.adaptiq.util.ExamSessionManager;
import com.adaptiq.util.SceneNavigator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.util.Duration;

/**
 * Distraction-free normal-exam controller.
 * Evaluation is handled by ExamService and ResultDAO.
 */
public final class ExamController {

    private final ExamService exams = new ExamService();
    private final Map<Long, Long> answers = new HashMap<>();

    private NormalExamSession session;
    private int index = 0;
    private Timeline timer;
    private boolean submitting = false;

    @FXML private Label titleLabel;
    @FXML private Label timerLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionLabel;
    @FXML private Label statusLabel;

    @FXML private RadioButton optionA;
    @FXML private RadioButton optionB;
    @FXML private RadioButton optionC;
    @FXML private RadioButton optionD;

    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Button submitButton;

    @FXML
    private void initialize() {

        session = ExamSessionManager.current()
                .orElseThrow(() ->
                        new IllegalStateException("No active examination."));

        titleLabel.setText(session.exam().getTitle());

        System.out.println("========== EXAM DEBUG ==========");
        System.out.println("Java timezone: " +
                java.time.ZoneId.systemDefault());
        System.out.println("Exam ID: " +
                session.exam().getExamId());
        System.out.println("Attempt ID: " +
                session.attempt().getAttemptId());
        System.out.println("Started at: " +
                session.attempt().getStartedAt());
        System.out.println("Duration: " +
                session.exam().getDurationMinutes());
        System.out.println("Remaining seconds: " +
                session.remainingSeconds(Instant.now()));
        System.out.println("Questions: " +
                session.questions().size());
        System.out.println("================================");

        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> tick())
        );

        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        tick();
        showQuestion();
    }

    @FXML
    private void previous() {
        capture();

        if (index > 0) {
            index--;
            showQuestion();
        }
    }

    @FXML
    private void next() {
        capture();

        if (index < session.questions().size() - 1) {
            index++;
            showQuestion();
        }
    }

    @FXML
    private void submit() {

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Submit this examination now?",
                ButtonType.CANCEL,
                ButtonType.OK
        );

        confirmation.setHeaderText("Confirm submission");

        if (confirmation.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .isPresent()) {

            capture();
            submitExam(false);
        }
    }

    private void tick() {

        long seconds = session.remainingSeconds(Instant.now());

        timerLabel.setText(
                String.format("%02d:%02d", seconds / 60, seconds % 60)
        );

        if (seconds <= 0 && !submitting) {
            timer.stop();
            capture();
            submitExam(true);
        }
    }

    private void showQuestion() {

        ExamQuestionView question =
                session.questions().get(index);

        questionNumberLabel.setText(
                "Question " + (index + 1)
                        + " of " + session.questions().size()
                        + " • " + question.marks() + " marks"
        );

        questionLabel.setText(question.questionText());

        RadioButton[] buttons = {
                optionA, optionB, optionC, optionD
        };

        for (int i = 0; i < 4; i++) {

            ExamQuestionView.OptionView option =
                    question.options().get(i);

            buttons[i].setText(
                    option.optionLabel() + ". " + option.optionText()
            );

            buttons[i].setUserData(option.optionId());

            buttons[i].setSelected(
                    answers.getOrDefault(question.questionId(), -1L)
                            .equals(option.optionId())
            );
        }

        previousButton.setDisable(index == 0);

        nextButton.setDisable(
                index == session.questions().size() - 1
        );
    }

    private void capture() {

        ExamQuestionView question =
                session.questions().get(index);

        RadioButton[] buttons = {
                optionA, optionB, optionC, optionD
        };

        for (RadioButton button : buttons) {

            if (button.isSelected()) {

                answers.put(
                        question.questionId(),
                        (Long) button.getUserData()
                );

                return;
            }
        }
    }

    private void submitExam(boolean expired) {

        if (submitting) {
            return;
        }

        submitting = true;

        if (timer != null) {
            timer.stop();
        }

        submitButton.setDisable(true);

        statusLabel.setText(
                expired ? "Time expired. Submitting..."
                        : "Submitting examination..."
        );

        Task<EvaluationResult> task = new Task<>() {

            @Override
            protected EvaluationResult call() {

                System.out.println("========== SUBMISSION DEBUG ==========");
                System.out.println("Attempt ID: " +
                        session.attempt().getAttemptId());
                System.out.println("Answers to save: " + answers.size());

                for (Map.Entry<Long, Long> answer :
                        answers.entrySet()) {

                    System.out.println(
                            "Saving question ID: " + answer.getKey()
                                    + ", option ID: " + answer.getValue()
                    );

                    exams.saveNormalAnswer(
                            session.attempt().getAttemptId(),
                            answer.getKey(),
                            answer.getValue()
                    );
                }

                System.out.println("All answers saved.");
                System.out.println("Submitting attempt...");

                return exams.submitNormal(
                        session.attempt().getAttemptId(),
                        expired
                );
            }
        };

        task.setOnSucceeded(event -> {

            EvaluationResult result = task.getValue();

            System.out.println("Examination submitted successfully.");

            ExamSessionManager.clear();

            Alert resultAlert = new Alert(
                    Alert.AlertType.INFORMATION,
                    "Score: " + result.obtainedMarks()
                            + " / " + result.totalMarks()
                            + "\nPercentage: " + result.percentage() + "%"
                            + "\nCorrect: " + result.correct()
                            + ", Incorrect: " + result.incorrect()
                            + ", Unanswered: " + result.unanswered(),
                    ButtonType.OK
            );

            resultAlert.setHeaderText("Examination Result");

            resultAlert.showAndWait();

            SceneNavigator.show("student_dashboard");
        });

        task.setOnFailed(event -> {

            System.err.println("========== EXAM SUBMISSION FAILED ==========");

            Throwable error = task.getException();

            if (error != null) {
                error.printStackTrace();
                statusLabel.setText(error.getMessage() == null
                        ? "Submission failed. Check the terminal."
                        : error.getMessage());
            } else {
                statusLabel.setText("Submission failed. Check the terminal.");
            }

            System.err.println("=============================================");

            submitting = false;
            submitButton.setDisable(false);
        });

        Thread thread = new Thread(
                task,
                "adaptiq-submit-exam"
        );

        thread.setDaemon(true);
        thread.start();
    }
}
