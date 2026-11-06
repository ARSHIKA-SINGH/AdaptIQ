package com.adaptiq.controller;

import com.adaptiq.model.Recommendation;
import com.adaptiq.service.RecommendationService;
import com.adaptiq.util.SceneNavigator;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class RecommendationsController {
    private final RecommendationService recommendations=new RecommendationService();
    @FXML private ListView<Recommendation> recommendationList; @FXML private Label statusLabel;
    @FXML private void initialize(){load(false);}
    @FXML private void refresh(){load(true);}
    @FXML private void complete(){Recommendation x=recommendationList.getSelectionModel().getSelectedItem();if(x==null){statusLabel.setText("Select a recommendation to complete.");return;}Task<Void> task=new Task<>(){@Override protected Void call(){recommendations.complete(x.getRecommendationId());return null;}};task.setOnSucceeded(e->load(false));task.setOnFailed(e->statusLabel.setText(task.getException()==null?"Could not update recommendation.":task.getException().getMessage()));Thread t=new Thread(task,"adaptiq-recommendations");t.setDaemon(true);t.start();}
    @FXML private void back(){SceneNavigator.show("student_dashboard");}
    private void load(boolean regenerate){Task<List<Recommendation>> task=new Task<>(){@Override protected List<Recommendation> call(){return regenerate?recommendations.refreshMine():recommendations.mine();}};task.setOnSucceeded(e->{recommendationList.setItems(FXCollections.observableArrayList(task.getValue()));statusLabel.setText(task.getValue().isEmpty()?"No recommendations yet. Complete an examination and refresh.":"");});task.setOnFailed(e->statusLabel.setText(task.getException()==null?"Could not load recommendations.":task.getException().getMessage()));Thread t=new Thread(task,"adaptiq-recommendations");t.setDaemon(true);t.start();}
}
