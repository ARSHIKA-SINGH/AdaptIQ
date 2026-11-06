package com.adaptiq.controller;

import com.adaptiq.service.AnalyticsService;
import com.adaptiq.service.AnalyticsSnapshot;
import com.adaptiq.service.PdfReportService;
import com.adaptiq.util.SceneNavigator;
import java.math.BigDecimal;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;

public final class AnalyticsController {
    private final AnalyticsService analytics=new AnalyticsService();
    @FXML private Label testsLabel,averageLabel,highestLabel,lowestLabel,accuracyLabel,statusLabel;
    @FXML private BarChart<String,Number> topicChart,difficultyChart; @FXML private LineChart<String,Number> trendChart; @FXML private PieChart accuracyChart;
    @FXML private void initialize(){refresh();}
    @FXML private void refresh(){Task<AnalyticsSnapshot> task=new Task<>(){@Override protected AnalyticsSnapshot call(){return analytics.myAnalytics();}};task.setOnSucceeded(e->populate(task.getValue()));task.setOnFailed(e->statusLabel.setText(task.getException()==null?"Could not load analytics.":task.getException().getMessage()));Thread t=new Thread(task,"adaptiq-analytics");t.setDaemon(true);t.start();}
    @FXML private void back(){SceneNavigator.show("student_dashboard");}
    @FXML private void exportPdf(){File file=chooser("student-performance-report.pdf").showSaveDialog(statusLabel.getScene().getWindow());if(file==null)return;Task<Void> task=new Task<>(){@Override protected Void call(){new PdfReportService().generateStudentReport(file.toPath());return null;}};task.setOnSucceeded(e->statusLabel.setText("Report saved to "+file.getName()));task.setOnFailed(e->statusLabel.setText(task.getException()==null?"Could not create report.":task.getException().getMessage()));Thread t=new Thread(task,"adaptiq-student-report");t.setDaemon(true);t.start();}
    private void populate(AnalyticsSnapshot x){testsLabel.setText(Long.toString(x.testsAttempted()));averageLabel.setText(number(x.averageScore()));highestLabel.setText(number(x.highestScore()));lowestLabel.setText(number(x.lowestScore()));accuracyLabel.setText(number(x.overallAccuracy())+"%");topicChart.getData().setAll(series("Topic accuracy",x.topicAccuracy()));difficultyChart.getData().setAll(series("Difficulty accuracy",x.difficultyAccuracy()));XYChart.Series<String,Number> trend=new XYChart.Series<>();trend.setName("Percentage");x.performanceTrend().forEach(point->trend.getData().add(new XYChart.Data<>(point.dateLabel(),point.percentage())));trendChart.getData().setAll(trend);BigDecimal accuracy=x.overallAccuracy()==null?BigDecimal.ZERO:x.overallAccuracy();accuracyChart.setData(FXCollections.observableArrayList(new PieChart.Data("Correct ratio",accuracy.doubleValue()),new PieChart.Data("Incorrect ratio",BigDecimal.valueOf(100).subtract(accuracy).max(BigDecimal.ZERO).doubleValue())));statusLabel.setText(x.testsAttempted()==0?"No submitted examinations yet.":"");}
    private XYChart.Series<String,Number> series(String title,java.util.List<AnalyticsSnapshot.NamedAccuracy> data){XYChart.Series<String,Number> series=new XYChart.Series<>();series.setName(title);data.forEach(item->series.getData().add(new XYChart.Data<>(item.name(),item.accuracy())));return series;}
    private String number(BigDecimal value){return value==null?"0":value.stripTrailingZeros().toPlainString();}
    private FileChooser chooser(String name){FileChooser chooser=new FileChooser();chooser.setInitialFileName(name);chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF document","*.pdf"));return chooser;}
}
