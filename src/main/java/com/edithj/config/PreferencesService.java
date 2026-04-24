package com.edithj.config;

import java.util.prefs.Preferences;


public final class PreferencesService {

    private static final PreferencesService INSTANCE = new PreferencesService();

    private static final String KEY_AUTO_SEND_VOICE_INPUT = "voice.autoSendInput";
    private static final String KEY_PREFER_SHORTCUT_APPS = "launcher.preferShortcutApps";
    private static final String KEY_ALLOW_WEB_FALLBACK = "launcher.allowWebFallback";
    private static final String KEY_WHATSAPP_APP_FIRST = "launcher.whatsappAppFirst";
    private static final String KEY_DEV_SMOKE_LAUNCHERS_ENABLED = "dev.smokeLaunchersEnabled";

    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    private boolean autoSendVoiceInput;
    private boolean preferShortcutApps;
    private boolean allowWebFallback;
    private boolean whatsappAppFirst;
    private boolean devSmokeLaunchersEnabled;

    private PreferencesService() {
        autoSendVoiceInput = preferences.getBoolean(KEY_AUTO_SEND_VOICE_INPUT, true);
        preferShortcutApps = preferences.getBoolean(KEY_PREFER_SHORTCUT_APPS, true);
        allowWebFallback = preferences.getBoolean(KEY_ALLOW_WEB_FALLBACK, true);
        whatsappAppFirst = preferences.getBoolean(KEY_WHATSAPP_APP_FIRST, true);
        
        boolean defaultSmokeLaunchers = AppConfig.load().isDevSmokeLaunchersEnabled();
        devSmokeLaunchersEnabled = preferences.getBoolean(KEY_DEV_SMOKE_LAUNCHERS_ENABLED, defaultSmokeLaunchers);
    }

    public static PreferencesService instance() {
        return INSTANCE;
    }

    public boolean isAutoSendVoiceInputEnabled() {
        return autoSendVoiceInput;
    }

    public void setAutoSendVoiceInputEnabled(boolean enabled) {
        this.autoSendVoiceInput = enabled;
        preferences.putBoolean(KEY_AUTO_SEND_VOICE_INPUT, enabled);
    }

    public boolean isPreferShortcutAppsEnabled() {
        return preferShortcutApps;
    }

    public void setPreferShortcutAppsEnabled(boolean enabled) {
        this.preferShortcutApps = enabled;
        preferences.putBoolean(KEY_PREFER_SHORTCUT_APPS, enabled);
    }

    public boolean isWebFallbackAllowed() {
        return allowWebFallback;
    }

    public void setWebFallbackAllowed(boolean enabled) {
        this.allowWebFallback = enabled;
        preferences.putBoolean(KEY_ALLOW_WEB_FALLBACK, enabled);
    }

    public boolean isWhatsAppAppFirstEnabled() {
        return whatsappAppFirst;
    }

    public void setWhatsAppAppFirstEnabled(boolean enabled) {
        this.whatsappAppFirst = enabled;
        preferences.putBoolean(KEY_WHATSAPP_APP_FIRST, enabled);
    }

    public boolean isDevSmokeLaunchersEnabled() {
        return devSmokeLaunchersEnabled;
    }

    public void setDevSmokeLaunchersEnabled(boolean enabled) {
        this.devSmokeLaunchersEnabled = enabled;
        preferences.putBoolean(KEY_DEV_SMOKE_LAUNCHERS_ENABLED, enabled);
    }
}
