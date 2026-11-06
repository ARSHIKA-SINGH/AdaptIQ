package com.adaptiq.controller;

import com.adaptiq.dao.QuestionFilter;
import com.adaptiq.model.Exam;
import com.adaptiq.model.Question;
import com.adaptiq.model.Subject;
import com.adaptiq.service.CatalogService;
import com.adaptiq.service.ExamService;
import com.adaptiq.service.QuestionService;
import com.adaptiq.util.SceneNavigator;
import java.math.BigDecimal;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class ExamBuilderController {
    private final CatalogService catalog=new CatalogService(); private final QuestionService questions=new QuestionService(); private final ExamService exams=new ExamService();
    @FXML private ChoiceBox<Subject> subjectChoice; @FXML private ListView<Question> questionList; @FXML private TextField titleField,durationField; @FXML private TextArea descriptionArea; @FXML private Label statusLabel,totalLabel;
    @FXML private void initialize(){questionList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);run(new Task<List<Subject>>(){@Override protected List<Subject> call(){return catalog.subjects();}},values->subjectChoice.setItems(FXCollections.observableArrayList(values)));}
    @FXML private void loadQuestions(){Subject subject=subjectChoice.getValue();if(subject==null)return;run(new Task<List<Question>>(){@Override protected List<Question> call(){return questions.search(new QuestionFilter(subject.getSubjectId(),null,null,null));}},values->{questionList.setItems(FXCollections.observableArrayList(values));updateTotal();});}
    @FXML private void updateTotal(){BigDecimal total=questionList.getSelectionModel().getSelectedItems().stream().map(Question::getMarks).reduce(BigDecimal.ZERO,BigDecimal::add);totalLabel.setText("Total marks: "+total);}
    @FXML private void create(){try{Subject subject=subjectChoice.getValue();List<Question> selected=List.copyOf(questionList.getSelectionModel().getSelectedItems());Exam exam=new Exam();exam.setSubjectId(subject.getSubjectId());exam.setTitle(titleField.getText().trim());exam.setDescription(descriptionArea.getText().trim());exam.setDurationMinutes(Integer.parseInt(durationField.getText().trim()));exam.setTotalMarks(selected.stream().map(Question::getMarks).reduce(BigDecimal.ZERO,BigDecimal::add));run(new Task<Long>(){@Override protected Long call(){return exams.createNormalExam(exam,selected.stream().map(Question::getQuestionId).toList());}},id->{statusLabel.setText("Published examination #"+id+" created.");});}catch(Exception e){statusLabel.setText("Choose a subject, questions, title, and valid duration.");}}
    @FXML private void back(){SceneNavigator.show("faculty_dashboard");}
    private <T>void run(Task<T>task,java.util.function.Consumer<T>success){statusLabel.setText("Loading…");task.setOnSucceeded(e->{statusLabel.setText("");success.accept(task.getValue());});task.setOnFailed(e->statusLabel.setText(task.getException()==null?"Operation failed.":task.getException().getMessage()));Thread t=new Thread(task,"adaptiq-exam-builder");t.setDaemon(true);t.start();}
}
