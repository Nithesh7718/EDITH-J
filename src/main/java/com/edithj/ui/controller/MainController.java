package com.edithj.ui.controller;

import com.edithj.ui.navigation.SceneManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML private StackPane contentHost;
    @FXML private Label sectionTitle;
    @FXML private Label clockLabel;
    @FXML private Button btnChat;
    @FXML private Button btnNotes;
    @FXML private Button btnReminders;

    private final SceneManager sceneManager = new SceneManager();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");


    @FXML
    private void initialize() {
        startLiveClock();
        startStatusPulse();
        showChatView();
    }

    /** Ticks the footer clock every second */
    private void startLiveClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
            clockLabel.setText(LocalTime.now().format(timeFormat))
        ));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        // Set initial value immediately
        clockLabel.setText(LocalTime.now().format(timeFormat));
    }

    /** Gently pulses the section title to feel "alive" */
    private void startStatusPulse() {
        FadeTransition pulse = new FadeTransition(Duration.seconds(2.5), sectionTitle);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.45);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
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

    /** Updates the sidebar active state styling */
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

    /** Loads content with a cinematic entry transition */
    private void loadContent(String resourcePath) {
        Node content = sceneManager.loadView(resourcePath);
        if (content == null) return;

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
}
