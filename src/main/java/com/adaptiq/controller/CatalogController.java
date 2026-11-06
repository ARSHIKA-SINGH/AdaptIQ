package com.adaptiq.controller;

import com.adaptiq.model.Subject;
import com.adaptiq.model.Topic;
import com.adaptiq.service.CatalogService;
import com.adaptiq.util.SceneNavigator;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Administrator subject and topic maintenance controller. */
public final class CatalogController {
    private final CatalogService catalogService = new CatalogService();
    @FXML private ListView<Subject> subjectList;
    @FXML private ListView<Topic> topicList;
    @FXML private TextField subjectNameField, topicNameField;
    @FXML private TextArea subjectDescriptionArea, topicDescriptionArea;
    @FXML private Label statusLabel;
    @FXML private void initialize() { subjectList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, subject) -> selectSubject(subject)); topicList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, topic) -> selectTopic(topic)); refreshSubjects(); }
    @FXML private void refreshSubjects() { run(new Task<List<Subject>>() { @Override protected List<Subject> call() { return catalogService.subjects(); } }, subjects -> subjectList.setItems(FXCollections.observableArrayList(subjects))); }
    @FXML private void saveSubject() { Subject subject = subjectList.getSelectionModel().getSelectedItem(); if (subject == null) subject = new Subject(); subject.setSubjectName(subjectNameField.getText().trim()); subject.setDescription(subjectDescriptionArea.getText().trim()); Subject saved = subject; run(new Task<Long>() { @Override protected Long call() { return catalogService.saveSubject(saved); } }, id -> { statusLabel.setText("Subject saved."); clearSubject(); refreshSubjects(); }); }
    @FXML private void deleteSubject() { Subject subject = subjectList.getSelectionModel().getSelectedItem(); if (subject == null) { statusLabel.setText("Select a subject to delete."); return; } confirm("Delete the selected subject? It must not have related topics or questions.").ifPresent(ignored -> run(new Task<Void>() { @Override protected Void call() { catalogService.deleteSubject(subject.getSubjectId()); return null; } }, value -> { clearSubject(); topicList.getItems().clear(); refreshSubjects(); })); }
    @FXML private void saveTopic() { Subject subject = subjectList.getSelectionModel().getSelectedItem(); if (subject == null) { statusLabel.setText("Select a subject first."); return; } Topic topic = topicList.getSelectionModel().getSelectedItem(); if (topic == null) topic = new Topic(); topic.setSubjectId(subject.getSubjectId()); topic.setTopicName(topicNameField.getText().trim()); topic.setDescription(topicDescriptionArea.getText().trim()); Topic saved = topic; run(new Task<Long>() { @Override protected Long call() { return catalogService.saveTopic(saved); } }, id -> { statusLabel.setText("Topic saved."); clearTopic(); loadTopics(subject); }); }
    @FXML private void deleteTopic() { Topic topic = topicList.getSelectionModel().getSelectedItem(); if (topic == null) { statusLabel.setText("Select a topic to delete."); return; } confirm("Delete the selected topic?").ifPresent(ignored -> run(new Task<Void>() { @Override protected Void call() { catalogService.deleteTopic(topic.getTopicId()); return null; } }, value -> { Subject subject=subjectList.getSelectionModel().getSelectedItem(); clearTopic(); if (subject != null) loadTopics(subject); })); }
    @FXML private void clearSubject() { subjectList.getSelectionModel().clearSelection(); subjectNameField.clear(); subjectDescriptionArea.clear(); }
    @FXML private void clearTopic() { topicList.getSelectionModel().clearSelection(); topicNameField.clear(); topicDescriptionArea.clear(); }
    @FXML private void returnToDashboard() { SceneNavigator.show("admin_dashboard"); }
    private void selectSubject(Subject subject) { if (subject == null) return; subjectNameField.setText(subject.getSubjectName()); subjectDescriptionArea.setText(subject.getDescription()); loadTopics(subject); }
    private void selectTopic(Topic topic) { if (topic == null) return; topicNameField.setText(topic.getTopicName()); topicDescriptionArea.setText(topic.getDescription()); }
    private void loadTopics(Subject subject) { run(new Task<List<Topic>>() { @Override protected List<Topic> call() { return catalogService.topics(subject.getSubjectId()); } }, topics -> topicList.setItems(FXCollections.observableArrayList(topics))); }
    private java.util.Optional<ButtonType> confirm(String message) { Alert alert=new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK); return alert.showAndWait().filter(answer -> answer == ButtonType.OK); }
    private <T> void run(Task<T> task, java.util.function.Consumer<T> success) { statusLabel.setText("Loading…"); task.setOnSucceeded(event -> { statusLabel.setText(""); success.accept(task.getValue()); }); task.setOnFailed(event -> statusLabel.setText(task.getException() == null ? "Operation failed." : task.getException().getMessage())); Thread thread=new Thread(task, "adaptiq-catalog"); thread.setDaemon(true); thread.start(); }
}
