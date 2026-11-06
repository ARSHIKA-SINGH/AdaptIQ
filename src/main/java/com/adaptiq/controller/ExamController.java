package com.adaptiq.controller;

import com.adaptiq.model.ExamQuestionView;
import com.adaptiq.service.EvaluationResult;
import com.adaptiq.service.ExamService;
import com.adaptiq.service.NormalExamSession;
import com.adaptiq.util.ExamSessionManager;
import com.adaptiq.util.SceneNavigator;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

/** Distraction-free normal-exam controller. Evaluation never occurs in this controller. */
public final class ExamController {
    private final ExamService exams=new ExamService(); private final Map<Long,Long> answers=new HashMap<>();
    private NormalExamSession session; private int index; private Timeline timer; private boolean submitting;
    @FXML private Label titleLabel, timerLabel, questionNumberLabel, questionLabel, statusLabel;
    @FXML private RadioButton optionA,optionB,optionC,optionD; @FXML private Button previousButton,nextButton,submitButton;
    @FXML private void initialize(){session=ExamSessionManager.current().orElseThrow(()->new IllegalStateException("No active examination."));titleLabel.setText(session.exam().getTitle());timer=new Timeline(new KeyFrame(Duration.seconds(1),event->tick()));timer.setCycleCount(Timeline.INDEFINITE);timer.play();showQuestion();}
    @FXML private void previous(){capture();if(index>0){index--;showQuestion();}}
    @FXML private void next(){capture();if(index<session.questions().size()-1){index++;showQuestion();}}
    @FXML private void submit(){if(new Alert(Alert.AlertType.CONFIRMATION,"Submit this examination now?",ButtonType.CANCEL,ButtonType.OK).showAndWait().filter(button->button==ButtonType.OK).isPresent()){capture();submit(false);}}
    private void tick(){long seconds=session.remainingSeconds(Instant.now());timerLabel.setText(String.format("%02d:%02d",seconds/60,seconds%60));if(seconds<=0){timer.stop();capture();submit(true);}}
    private void showQuestion(){ExamQuestionView q=session.questions().get(index);questionNumberLabel.setText("Question "+(index+1)+" of "+session.questions().size()+" • "+q.marks()+" marks");questionLabel.setText(q.questionText());RadioButton[] buttons={optionA,optionB,optionC,optionD};for(int i=0;i<4;i++){ExamQuestionView.OptionView option=q.options().get(i);buttons[i].setText(option.optionLabel()+". "+option.optionText());buttons[i].setUserData(option.optionId());buttons[i].setSelected(answers.getOrDefault(q.questionId(),-1L).equals(option.optionId()));}previousButton.setDisable(index==0);nextButton.setDisable(index==session.questions().size()-1);}
    private void capture(){ExamQuestionView q=session.questions().get(index);RadioButton[] buttons={optionA,optionB,optionC,optionD};for(RadioButton button:buttons)if(button.isSelected()){answers.put(q.questionId(),(Long)button.getUserData());return;}}
    private void submit(boolean expired){if(submitting)return;submitting=true;timer.stop();submitButton.setDisable(true);statusLabel.setText(expired?"Time expired. Submitting…":"Submitting…");Task<EvaluationResult> task=new Task<>(){@Override protected EvaluationResult call(){for(Map.Entry<Long,Long> answer:answers.entrySet())exams.saveNormalAnswer(session.attempt().getAttemptId(),answer.getKey(),answer.getValue());return exams.submitNormal(session.attempt().getAttemptId(),expired);}};task.setOnSucceeded(event->{EvaluationResult result=task.getValue();ExamSessionManager.clear();new Alert(Alert.AlertType.INFORMATION,"Score: "+result.obtainedMarks()+" / "+result.totalMarks()+"\nPercentage: "+result.percentage()+"%\nCorrect: "+result.correct()+", Incorrect: "+result.incorrect()+", Unanswered: "+result.unanswered(),ButtonType.OK).showAndWait();SceneNavigator.show("student_dashboard");});task.setOnFailed(event->{submitting=false;submitButton.setDisable(false);statusLabel.setText(task.getException()==null?"Submission failed.":task.getException().getMessage());});Thread thread=new Thread(task,"adaptiq-submit-exam");thread.setDaemon(true);thread.start();}
}
