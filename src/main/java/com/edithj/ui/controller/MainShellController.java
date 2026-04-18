package com.edithj.ui.controller;

import java.util.concurrent.CompletableFuture;

import com.edithj.assistant.AssistantStatus;
import com.edithj.assistant.AssistantStatusProbe;
import com.edithj.assistant.AssistantStatusService;
import com.edithj.ui.navigation.SceneManager;
import com.edithj.ui.session.UiAssistantGateway;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class MainShellController {

    @FXML
    private StackPane contentHost;
    @FXML
    private Label statusBadgeLabel;
    @FXML
    private Button btnChat;
    @FXML
    private Button btnNotes;
    @FXML
    private Button btnReminders;
    @FXML
    private Button btnDesktopTools;
    @FXML
    private Button btnSettings;

    private final SceneManager sceneManager = new SceneManager();
    private final AssistantStatusService statusService = AssistantStatusService.instance();
    private final AssistantStatusProbe statusProbe = new AssistantStatusProbe();
    private final UiAssistantGateway assistantGateway = UiAssistantGateway.instance();

    @FXML
    private void initialize() {
        statusBadgeLabel.textProperty().bind(assistantGateway.statusTextProperty());
        refreshStatusBadge();
        runStartupStatusProbe();
        showChatView();
    }

    @FXML
    private void showChatView() {
        setActiveNav(btnChat);
        loadContent("/fxml/chat-view.fxml");
    }

    @FXML
    private void showNotesView() {
        setActiveNav(btnNotes);
        loadContent("/fxml/notes-view.fxml");
    }

    @FXML
    private void showRemindersView() {
        setActiveNav(btnReminders);
        loadContent("/fxml/reminders-view.fxml");
    }

    @FXML
    private void showDesktopToolsView() {
        setActiveNav(btnDesktopTools);
        loadContent("/fxml/desktop-tools-view.fxml");
    }

    @FXML
    private void showSettingsView() {
        setActiveNav(btnSettings);
        loadContent("/fxml/settings-view.fxml");
    }

    @FXML
    private void openSettingsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("EDITH-J settings");
        alert.setContentText("API key loaded: " + (statusService.status() == AssistantStatus.ONLINE ? "yes" : "check configuration")
                + "\nTheme toggle is available on the Settings page.");
        alert.showAndWait();
    }

    @FXML
    private void openHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("What can EDITH do?");
        alert.setHeaderText("Command examples");
        alert.setContentText("""
            Notes: note buy milk, list notes
            Reminders: remind me to call mom at 7 PM
            Desktop tools: show clipboard, open downloads
            Focus: start focus 25, focus status, end focus
            Tasks: add task submit report, done task 1
            """);
        alert.showAndWait();
    }

    private void setActiveNav(Button selected) {
        for (Button btn : new Button[]{btnChat, btnNotes, btnReminders, btnDesktopTools, btnSettings}) {
            btn.getStyleClass().removeAll("nav-button", "nav-button-active", "nav-button-muted");
            if (btn == selected) {
                btn.getStyleClass().add("nav-button-active");
            } else {
                btn.getStyleClass().add("nav-button-muted");
            }
        }
    }

    private void loadContent(String resourcePath) {
        Node content = sceneManager.loadView(resourcePath);
        if (content == null) {
            return;
        }

        contentHost.getChildren().setAll(content);
        content.setOpacity(0);
        content.setTranslateY(20);
        content.setScaleX(0.97);
        content.setScaleY(0.97);

        FadeTransition fade = new FadeTransition(Duration.millis(450), content);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(450), content);
        slide.setFromY(20);
        slide.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(450), content);
        scale.setFromX(0.97);
        scale.setFromY(0.97);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition entry = new ParallelTransition(fade, slide, scale);
        entry.setInterpolator(Interpolator.SPLINE(0.2, 0.0, 0.2, 1.0));
        entry.play();
    }

    private void runStartupStatusProbe() {
        CompletableFuture.runAsync(() -> {
            statusProbe.runStartupProbe();
            Platform.runLater(this::refreshStatusBadge);
        });
    }

    private void refreshStatusBadge() {
        AssistantStatus status = statusService.status();
        statusBadgeLabel.getStyleClass().remove("status-badge-offline");
        if (status == AssistantStatus.OFFLINE && !assistantGateway.thinkingProperty().get()) {
            assistantGateway.setReadyMessage("Offline - Groq unreachable");
            statusBadgeLabel.getStyleClass().add("status-badge-offline");
        } else if (!assistantGateway.thinkingProperty().get()) {
            assistantGateway.setReadyMessage("Ready");
        }
    }
}
