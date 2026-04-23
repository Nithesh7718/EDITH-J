package com.edithj.ui.controller;

import com.edithj.config.AppConfig;
import com.edithj.config.ModelConfig;
import com.edithj.ui.session.ThemeService;
import com.edithj.ui.session.ThemeService.Theme;
import com.edithj.ui.session.UiPreferencesService;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

@SuppressWarnings("unused")
public class SettingsViewController {

    @FXML
    private Label apiKeyStatusLabel;
    @FXML
    private Label modelLabel;
    @FXML
    private Label environmentLabel;
    @FXML
    private Label versionLabel;
    @FXML
    private CheckBox preferShortcutAppsCheckBox;
    @FXML
    private CheckBox allowWebFallbackCheckBox;
    @FXML
    private CheckBox whatsappAppFirstCheckBox;
    @FXML
    private CheckBox devSmokeLaunchersCheckBox;
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
        AppConfig config = AppConfig.load();
        ModelConfig modelConfig = config.modelConfig();

        boolean hasApiKey = modelConfig.apiKey() != null && !modelConfig.apiKey().isBlank();
        apiKeyStatusLabel.setText(hasApiKey ? "Groq API key detected" : "Groq API key missing");

        if (modelLabel != null) {
            modelLabel.setText(modelConfig.model());
        }
        if (environmentLabel != null) {
            // Simple environment hint: presence of GROQ_API_KEY and base URL.
            String env = hasApiKey ? "Configured" : "Unconfigured";
            environmentLabel.setText(env + " • " + modelConfig.baseUrl());
        }
        if (versionLabel != null) {
            String implVersion = SettingsViewController.class.getPackage() == null
                    ? null
                    : SettingsViewController.class.getPackage().getImplementationVersion();
            versionLabel.setText(implVersion == null || implVersion.isBlank() ? "dev" : implVersion.trim());
        }

        preferShortcutAppsCheckBox.setSelected(uiPreferencesService.isPreferShortcutAppsEnabled());
        allowWebFallbackCheckBox.setSelected(uiPreferencesService.isWebFallbackAllowed());
        whatsappAppFirstCheckBox.setSelected(uiPreferencesService.isWhatsAppAppFirstEnabled());
        if (devSmokeLaunchersCheckBox != null) {
            // Master-gated by app config; user can only disable if config allows.
            devSmokeLaunchersCheckBox.setDisable(!config.isDevSmokeLaunchersEnabled());
            devSmokeLaunchersCheckBox.setSelected(uiPreferencesService.isDevSmokeLaunchersEnabled());
        }

        uiPreferencesService.preferShortcutAppsProperty().addListener((obs, oldValue, newValue) -> {
            if (preferShortcutAppsCheckBox.isSelected() != newValue) {
                preferShortcutAppsCheckBox.setSelected(newValue);
            }
        });
        uiPreferencesService.allowWebFallbackProperty().addListener((obs, oldValue, newValue) -> {
            if (allowWebFallbackCheckBox.isSelected() != newValue) {
                allowWebFallbackCheckBox.setSelected(newValue);
            }
        });
        uiPreferencesService.whatsappAppFirstProperty().addListener((obs, oldValue, newValue) -> {
            if (whatsappAppFirstCheckBox.isSelected() != newValue) {
                whatsappAppFirstCheckBox.setSelected(newValue);
            }
        });
        uiPreferencesService.devSmokeLaunchersEnabledProperty().addListener((obs, oldValue, newValue) -> {
            if (devSmokeLaunchersCheckBox != null && devSmokeLaunchersCheckBox.isSelected() != newValue) {
                devSmokeLaunchersCheckBox.setSelected(newValue);
            }
        });

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

    @FXML
    private void onPreferShortcutAppsToggled() {
        uiPreferencesService.setPreferShortcutAppsEnabled(preferShortcutAppsCheckBox.isSelected());
    }

    @FXML
    private void onAllowWebFallbackToggled() {
        uiPreferencesService.setWebFallbackAllowed(allowWebFallbackCheckBox.isSelected());
    }

    @FXML
    private void onWhatsAppAppFirstToggled() {
        uiPreferencesService.setWhatsAppAppFirstEnabled(whatsappAppFirstCheckBox.isSelected());
    }

    @FXML
    private void onDevSmokeLaunchersToggled() {
        if (devSmokeLaunchersCheckBox == null) {
            return;
        }
        uiPreferencesService.setDevSmokeLaunchersEnabled(devSmokeLaunchersCheckBox.isSelected());
    }

    private void updateThemeLabels(Theme theme) {
        boolean dark = theme == Theme.DARK;
        themeToggleButton.setSelected(!dark);
        themeToggleButton.setText(dark ? "Switch to Light Mode" : "Switch to Dark Mode");
        themeStatusLabel.setText(dark ? "Current theme: Dark" : "Current theme: Light");
    }
}
