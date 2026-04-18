package com.edithj.ui.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import com.edithj.assistant.AssistantStatus;
import com.edithj.assistant.AssistantStatusProbe;
import com.edithj.assistant.AssistantStatusService;
import com.edithj.ui.navigation.SceneManager;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class MainShellController {

    @FXML
    private StackPane contentHost;
    @FXML
    private Label sectionTitle;
    @FXML
    private Label clockLabel;
    @FXML
    private Label statusBadgeLabel;
    @FXML
    private Button btnChat;
    @FXML
    private Button btnNotes;
    @FXML
    private Button btnReminders;

    private final SceneManager sceneManager = new SceneManager();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final AssistantStatusService statusService = AssistantStatusService.instance();
    private final AssistantStatusProbe statusProbe = new AssistantStatusProbe();

    @FXML
    private void initialize() {
        startLiveClock();
        refreshStatusBadge();
        runStartupStatusProbe();
        showChatView();
    }

    @FXML
    private void showChatView() {
        setActiveNav(btnChat);
        sectionTitle.setText("CHAT");
        loadContent("/fxml/chat-view.fxml");
    }

    @FXML
    private void showNotesView() {
        setActiveNav(btnNotes);
        sectionTitle.setText("NOTES");
        loadContent("/fxml/notes-view.fxml");
    }

    @FXML
    private void showRemindersView() {
        setActiveNav(btnReminders);
        sectionTitle.setText("REMINDERS");
        loadContent("/fxml/reminders-view.fxml");
    }

    private void setActiveNav(Button selected) {
        for (Button btn : new Button[]{btnChat, btnNotes, btnReminders}) {
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

    private void startLiveClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), event
                -> clockLabel.setText(LocalTime.now().format(timeFormat))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        clockLabel.setText(LocalTime.now().format(timeFormat));
    }

    private void runStartupStatusProbe() {
        CompletableFuture.runAsync(() -> {
            statusProbe.runStartupProbe();
            Platform.runLater(this::refreshStatusBadge);
        });
    }

    private void refreshStatusBadge() {
        AssistantStatus status = statusService.status();
        String text = status == AssistantStatus.ONLINE
                ? "● ONLINE (AI READY)"
                : "● OFFLINE (GROQ UNREACHABLE)";
        statusBadgeLabel.setText(text);
        statusBadgeLabel.getStyleClass().remove("status-badge-offline");
        if (status == AssistantStatus.OFFLINE) {
            statusBadgeLabel.getStyleClass().add("status-badge-offline");
        }
    }
}
