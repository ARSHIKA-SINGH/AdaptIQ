package com.adaptiq.controller;

import com.adaptiq.model.Exam;
import com.adaptiq.service.ExamService;
import com.adaptiq.service.NormalExamSession;
import com.adaptiq.util.ExamSessionManager;
import com.adaptiq.util.SceneNavigator;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class AvailableExamsController {
    private final ExamService exams = new ExamService();
    @FXML private ListView<Exam> examList; @FXML private Label statusLabel;
    @FXML private void initialize(){refresh();}
    @FXML private void refresh(){run(new Task<List<Exam>>(){@Override protected List<Exam> call(){return exams.availableNormalExams();}}, values->examList.setItems(FXCollections.observableArrayList(values)));}
    @FXML private void start(){Exam exam=examList.getSelectionModel().getSelectedItem();if(exam==null){statusLabel.setText("Select an examination first.");return;}run(new Task<NormalExamSession>(){@Override protected NormalExamSession call(){return exams.startNormal(exam.getExamId());}},session->{ExamSessionManager.set(session);SceneNavigator.show("normal_exam");});}
    @FXML private void back(){SceneNavigator.show("student_dashboard");}
    private <T> void run(Task<T> task,java.util.function.Consumer<T> success){statusLabel.setText("Loading…");task.setOnSucceeded(e->{statusLabel.setText("");success.accept(task.getValue());});task.setOnFailed(e->statusLabel.setText(task.getException()==null?"Operation failed.":task.getException().getMessage()));Thread t=new Thread(task,"adaptiq-exams");t.setDaemon(true);t.start();}
}
