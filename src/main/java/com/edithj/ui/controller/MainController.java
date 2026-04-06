package com.edithj.ui.controller;

import com.edithj.ui.navigation.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

@SuppressWarnings("unused")
public class MainController {

    @FXML
    private StackPane contentHost;

    @FXML
    private Label sectionTitle;

    private final SceneManager sceneManager = new SceneManager();

    @FXML
    private void initialize() {
        showChatView();
    }

    @FXML
    private void showChatView() {
        loadContent("/fxml/chat-view.fxml", "Chat");
    }

    @FXML
    private void showNotesView() {
        loadContent("/fxml/notes-view.fxml", "Notes");
    }

    @FXML
    private void showRemindersView() {
        loadContent("/fxml/reminders-view.fxml", "Reminders");
    }

    private void loadContent(String resourcePath, String title) {
        Node content = sceneManager.loadView(resourcePath);
        contentHost.getChildren().setAll(content);
        sectionTitle.setText(title);
    }
}
