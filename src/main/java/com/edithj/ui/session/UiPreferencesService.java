package com.edithj.ui.session;

import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class UiPreferencesService {

    private static final UiPreferencesService INSTANCE = new UiPreferencesService();

    private static final String KEY_AUTO_SEND_VOICE_INPUT = "voice.autoSendInput";
    private static final String KEY_PREFER_SHORTCUT_APPS = "launcher.preferShortcutApps";
    private static final String KEY_ALLOW_WEB_FALLBACK = "launcher.allowWebFallback";
    private static final String KEY_WHATSAPP_APP_FIRST = "launcher.whatsappAppFirst";

    private final Preferences preferences = Preferences.userNodeForPackage(UiPreferencesService.class);
    private final BooleanProperty autoSendVoiceInput = new SimpleBooleanProperty(true);
    private final BooleanProperty preferShortcutApps = new SimpleBooleanProperty(true);
    private final BooleanProperty allowWebFallback = new SimpleBooleanProperty(true);
    private final BooleanProperty whatsappAppFirst = new SimpleBooleanProperty(true);

    private UiPreferencesService() {
        autoSendVoiceInput.set(preferences.getBoolean(KEY_AUTO_SEND_VOICE_INPUT, true));
        autoSendVoiceInput.addListener((obs, oldValue, newValue)
                -> preferences.putBoolean(KEY_AUTO_SEND_VOICE_INPUT, newValue));

        preferShortcutApps.set(preferences.getBoolean(KEY_PREFER_SHORTCUT_APPS, true));
        preferShortcutApps.addListener((obs, oldValue, newValue)
                -> preferences.putBoolean(KEY_PREFER_SHORTCUT_APPS, newValue));

        allowWebFallback.set(preferences.getBoolean(KEY_ALLOW_WEB_FALLBACK, true));
        allowWebFallback.addListener((obs, oldValue, newValue)
                -> preferences.putBoolean(KEY_ALLOW_WEB_FALLBACK, newValue));

        whatsappAppFirst.set(preferences.getBoolean(KEY_WHATSAPP_APP_FIRST, true));
        whatsappAppFirst.addListener((obs, oldValue, newValue)
                -> preferences.putBoolean(KEY_WHATSAPP_APP_FIRST, newValue));
    }

    public static UiPreferencesService instance() {
        return INSTANCE;
    }

    public BooleanProperty autoSendVoiceInputProperty() {
        return autoSendVoiceInput;
    }

    public boolean isAutoSendVoiceInputEnabled() {
        return autoSendVoiceInput.get();
    }

    public void setAutoSendVoiceInputEnabled(boolean enabled) {
        autoSendVoiceInput.set(enabled);
    }

    public BooleanProperty preferShortcutAppsProperty() {
        return preferShortcutApps;
    }

    public boolean isPreferShortcutAppsEnabled() {
        return preferShortcutApps.get();
    }

    public void setPreferShortcutAppsEnabled(boolean enabled) {
        preferShortcutApps.set(enabled);
    }

    public BooleanProperty allowWebFallbackProperty() {
        return allowWebFallback;
    }

    public boolean isWebFallbackAllowed() {
        return allowWebFallback.get();
    }

    public void setWebFallbackAllowed(boolean enabled) {
        allowWebFallback.set(enabled);
    }

    public BooleanProperty whatsappAppFirstProperty() {
        return whatsappAppFirst;
    }

    public boolean isWhatsAppAppFirstEnabled() {
        return whatsappAppFirst.get();
    }

    public void setWhatsAppAppFirstEnabled(boolean enabled) {
        whatsappAppFirst.set(enabled);
    }
}
