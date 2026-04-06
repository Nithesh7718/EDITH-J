package com.edithj.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class SceneManager {

    public static final String MAIN_VIEW = "/fxml/main-view.fxml";

    public void showMainWindow(Stage stage) {
        Scene scene = new Scene(loadView(MAIN_VIEW), 1200, 800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
        stage.setTitle("EDITH-J");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    public Parent loadView(String resourcePath) {
        URL resource = Objects.requireNonNull(getClass().getResource(resourcePath), "Missing view: " + resourcePath);
        try {
            return FXMLLoader.load(resource);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load view: " + resourcePath, exception);
        }
    }
}
