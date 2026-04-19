package com.edithj.ui.session;

import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class UiPreferencesService {

    private static final UiPreferencesService INSTANCE = new UiPreferencesService();

    private static final String KEY_AUTO_SEND_VOICE_INPUT = "voice.autoSendInput";

    private final Preferences preferences = Preferences.userNodeForPackage(UiPreferencesService.class);
    private final BooleanProperty autoSendVoiceInput = new SimpleBooleanProperty(true);

    private UiPreferencesService() {
        autoSendVoiceInput.set(preferences.getBoolean(KEY_AUTO_SEND_VOICE_INPUT, true));
        autoSendVoiceInput.addListener((obs, oldValue, newValue)
                -> preferences.putBoolean(KEY_AUTO_SEND_VOICE_INPUT, newValue));
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
}
