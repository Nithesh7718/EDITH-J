package com.edithj.ui.controller;

import com.edithj.config.AppConfig;
import com.edithj.ui.session.ThemeService;
import com.edithj.ui.session.ThemeService.Theme;
import com.edithj.ui.session.UiPreferencesService;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class SettingsViewController {

    @FXML
    private Label apiKeyStatusLabel;
    @FXML
    private Label themeStatusLabel;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private CheckBox voiceAutoSendCheckBox;

    private final ThemeService themeService = ThemeService.instance();
    private final UiPreferencesService uiPreferencesService = UiPreferencesService.instance();

    @FXML
    private void initialize() {
        boolean hasApiKey = AppConfig.load().modelConfig().apiKey() != null
                && !AppConfig.load().modelConfig().apiKey().isBlank();
        apiKeyStatusLabel.setText(hasApiKey ? "Groq API key detected" : "Groq API key missing");

        updateThemeLabels(themeService.currentTheme());
        themeService.themeProperty().addListener((obs, oldValue, newValue) -> updateThemeLabels(newValue));

        voiceAutoSendCheckBox.setSelected(uiPreferencesService.isAutoSendVoiceInputEnabled());
        uiPreferencesService.autoSendVoiceInputProperty().addListener((obs, oldValue, newValue) -> {
            if (voiceAutoSendCheckBox.isSelected() != newValue) {
                voiceAutoSendCheckBox.setSelected(newValue);
            }
        });
    }

    @FXML
    private void onToggleTheme() {
        if (themeToggleButton.getScene() == null) {
            return;
        }
        themeService.toggleTheme(themeToggleButton.getScene());
    }

    @FXML
    private void onVoiceAutoSendToggled() {
        uiPreferencesService.setAutoSendVoiceInputEnabled(voiceAutoSendCheckBox.isSelected());
    }

    private void updateThemeLabels(Theme theme) {
        boolean dark = theme == Theme.DARK;
        themeToggleButton.setSelected(!dark);
        themeToggleButton.setText(dark ? "Switch to Light Mode" : "Switch to Dark Mode");
        themeStatusLabel.setText(dark ? "Current theme: Dark" : "Current theme: Light");
    }
}
