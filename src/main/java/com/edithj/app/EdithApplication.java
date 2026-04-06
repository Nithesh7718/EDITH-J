package com.edithj.app;

import com.edithj.ui.navigation.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class EdithApplication extends Application {

    @Override
    public void start(Stage stage) {
        SceneManager sceneManager = new SceneManager();
        sceneManager.showMainWindow(stage);
    }
}
