package com.adaptiq;

import com.adaptiq.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/** JavaFX application bootstrap. */
public final class App extends Application {
    @Override
    public void start(Stage stage) {
        SceneNavigator.initialize(stage);
        SceneNavigator.show("login");
    }

    public static void launchApp(String[] args) {
        launch(args);
    }
}
