package com.adaptiq.util;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Centralised, controller-safe FXML navigation. */
public final class SceneNavigator {
    private static Stage stage;

    private SceneNavigator() { }

    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("AdaptIQ - Adaptive Learning & Intelligent Examination");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
    }

    public static void show(String view) {
        if (stage == null) {
            throw new IllegalStateException("Application stage has not been initialized.");
        }
        URL location = SceneNavigator.class.getResource("/fxml/" + view + ".fxml");
        if (location == null) {
            throw new IllegalArgumentException("Unknown view: " + view);
        }
        try {
            Scene scene = new Scene(FXMLLoader.load(location), 1080, 720);
            scene.getStylesheets().add(SceneNavigator.class.getResource("/css/application.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load view " + view, exception);
        }
    }
}
