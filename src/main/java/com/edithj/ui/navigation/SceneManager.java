package com.edithj.ui.navigation;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Manages scene creation and view loading for EDITH-J.
 *
 * <p>
 * The main shell is loaded from {@code /fxml/main-shell.fxml} with the EDITH
 * shell theme ({@code /css/friday-theme.css}) as the primary stylesheet. The
 * legacy {@code app.css} is also loaded as a secondary sheet so that all
 * existing sub-view styles remain intact.
 */
public class SceneManager {

    public static final String MAIN_VIEW = "/fxml/main-shell.fxml";

    public void showMainWindow(Stage stage) {
        Parent root = loadView(MAIN_VIEW);
        Scene scene = new Scene(root, 1280, 800);

        // Legacy stylesheet (retains sub-view styles: notes, reminders, tools, settings)
        URL appCss = getClass().getResource("/css/app.css");
        if (appCss != null) {
            scene.getStylesheets().add(appCss.toExternalForm());
        }

        // Primary EDITH shell theme (file name retained for compatibility)
        // Loaded last so it can unify the overall shell look.
        URL shellThemeCss = getClass().getResource("/css/friday-theme.css");
        if (shellThemeCss != null) {
            scene.getStylesheets().add(shellThemeCss.toExternalForm());
        }

        stage.setTitle("EDITH-J  —  Voice Console");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Loads a JavaFX Parent from the given classpath resource path.
     *
     * @param resourcePath absolute classpath path, e.g.
     * {@code /fxml/chat-view.fxml}
     * @return loaded Parent node
     * @throws IllegalStateException if the resource is missing or invalid
     */
    public Parent loadView(String resourcePath) {
        URL resource = getClass().getResource(resourcePath);
        Objects.requireNonNull(resource, "Missing view resource: " + resourcePath);
        try {
            return FXMLLoader.load(resource);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load view: " + resourcePath, exception);
        }
    }
}
