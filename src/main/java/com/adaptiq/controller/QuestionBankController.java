package com.adaptiq.controller;

import com.adaptiq.dao.QuestionFilter;
import com.adaptiq.model.Difficulty;
import com.adaptiq.model.Question;
import com.adaptiq.model.QuestionOption;
import com.adaptiq.model.Subject;
import com.adaptiq.model.Topic;
import com.adaptiq.service.CatalogService;
import com.adaptiq.service.QuestionService;
import com.adaptiq.util.SceneNavigator;
import java.math.BigDecimal;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Faculty question-bank controller; data manipulation is delegated to services. */
public final class QuestionBankController {
    private final QuestionService questionService = new QuestionService();
    private final CatalogService catalogService = new CatalogService();
    @FXML private ChoiceBox<Subject> subjectChoice;
    @FXML private ChoiceBox<Topic> topicChoice, questionTopicChoice;
    @FXML private ChoiceBox<Difficulty> difficultyChoice, questionDifficultyChoice;
    @FXML private TextField searchField, marksField, optionAField, optionBField, optionCField, optionDField;
    @FXML private ChoiceBox<String> correctOptionChoice;
    @FXML private TextArea questionTextArea, explanationTextArea;
    @FXML private TableView<Question> questionTable;
    @FXML private TableColumn<Question, String> questionColumn, topicColumn, difficultyColumn;
    @FXML private TableColumn<Question, BigDecimal> marksColumn;
    @FXML private Label statusLabel;
    private Question selected;

    @FXML private void initialize() {
        questionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getQuestionText()));
        topicColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getTopicName()));
        difficultyColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getDifficulty().name()));
        marksColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMarks()));
        difficultyChoice.setItems(FXCollections.observableArrayList(Difficulty.values()));
        questionDifficultyChoice.setItems(FXCollections.observableArrayList(Difficulty.values()));
        correctOptionChoice.setItems(FXCollections.observableArrayList("A", "B", "C", "D"));
        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, value) -> showQuestion(value));
        loadSubjects();
        search();
    }
    @FXML private void loadTopicsForSubject() { Subject subject = subjectChoice.getValue(); if (subject == null) return; run(new Task<List<Topic>>() { @Override protected List<Topic> call() { return catalogService.topics(subject.getSubjectId()); } }, topics -> { topicChoice.setItems(FXCollections.observableArrayList(topics)); questionTopicChoice.setItems(FXCollections.observableArrayList(topics)); }); }
    @FXML private void search() { Subject subject = subjectChoice.getValue(); Topic topic = topicChoice.getValue(); Difficulty difficulty = difficultyChoice.getValue(); String search = searchField.getText(); run(new Task<List<Question>>() { @Override protected List<Question> call() { return questionService.search(new QuestionFilter(subject == null ? null : subject.getSubjectId(), topic == null ? null : topic.getTopicId(), difficulty, search)); } }, questions -> questionTable.setItems(FXCollections.observableArrayList(questions))); }
    @FXML private void saveQuestion() {
        try {
            Question question = selected == null ? new Question() : selected;
            question.setQuestionText(questionTextArea.getText().trim()); question.setTopicId(questionTopicChoice.getValue().getTopicId()); question.setDifficulty(questionDifficultyChoice.getValue()); question.setMarks(new BigDecimal(marksField.getText().trim())); question.setExplanation(explanationTextArea.getText().trim());
            String correct = correctOptionChoice.getValue(); question.setOptions(List.of(option("A", optionAField.getText(), correct), option("B", optionBField.getText(), correct), option("C", optionCField.getText(), correct), option("D", optionDField.getText(), correct)));
            run(new Task<Long>() { @Override protected Long call() { return questionService.save(question); } }, id -> { statusLabel.setText("Question saved."); clearForm(); search(); });
        } catch (Exception error) { statusLabel.setText("Complete every question field with valid values."); }
    }
    @FXML private void deleteQuestion() { if (selected == null) { statusLabel.setText("Select a question to delete."); return; } Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Delete the selected question? This cannot be undone.", ButtonType.CANCEL, ButtonType.OK); confirmation.showAndWait().filter(result -> result == ButtonType.OK).ifPresent(result -> { long id = selected.getQuestionId(); run(new Task<Void>() { @Override protected Void call() { questionService.delete(id); return null; } }, ignored -> { clearForm(); search(); statusLabel.setText("Question deleted."); }); }); }
    @FXML private void clearForm() { selected = null; questionTable.getSelectionModel().clearSelection(); questionTextArea.clear(); explanationTextArea.clear(); marksField.clear(); optionAField.clear(); optionBField.clear(); optionCField.clear(); optionDField.clear(); questionTopicChoice.setValue(null); questionDifficultyChoice.setValue(null); correctOptionChoice.setValue(null); }
    @FXML private void returnToDashboard() { SceneNavigator.show("faculty_dashboard"); }
    private void loadSubjects() { run(new Task<List<Subject>>() { @Override protected List<Subject> call() { return catalogService.subjects(); } }, subjects -> subjectChoice.setItems(FXCollections.observableArrayList(subjects))); }
    private void showQuestion(Question question) { if (question == null) return; selected = question; questionTextArea.setText(question.getQuestionText()); questionTopicChoice.setValue(questionTopicChoice.getItems().stream().filter(topic -> topic.getTopicId().equals(question.getTopicId())).findFirst().orElse(null)); questionDifficultyChoice.setValue(question.getDifficulty()); marksField.setText(question.getMarks().toPlainString()); explanationTextArea.setText(question.getExplanation()); for (QuestionOption option : question.getOptions()) { if (option.getOptionLabel().equals("A")) optionAField.setText(option.getOptionText()); if (option.getOptionLabel().equals("B")) optionBField.setText(option.getOptionText()); if (option.getOptionLabel().equals("C")) optionCField.setText(option.getOptionText()); if (option.getOptionLabel().equals("D")) optionDField.setText(option.getOptionText()); if (option.isCorrect()) correctOptionChoice.setValue(option.getOptionLabel()); } }
    private QuestionOption option(String label, String text, String correct) { return new QuestionOption(null, null, label, text, label.equals(correct)); }
    private <T> void run(Task<T> task, java.util.function.Consumer<T> success) { statusLabel.setText("Loading…"); task.setOnSucceeded(event -> { statusLabel.setText(""); success.accept(task.getValue()); }); task.setOnFailed(event -> statusLabel.setText(task.getException() == null ? "Operation failed." : task.getException().getMessage())); Thread thread = new Thread(task, "adaptiq-question-bank"); thread.setDaemon(true); thread.start(); }
}
