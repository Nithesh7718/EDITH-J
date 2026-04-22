package com.edithj.speech;

import java.util.Optional;

import com.edithj.config.AppConfig;

public final class SpeechAudioConfig {

    public static final String SYSTEM_PROPERTY_INPUT_DEVICE_NAME = "edithj.audio.inputDeviceName";
    public static final String ENV_INPUT_DEVICE_NAME = "EDITHJ_AUDIO_INPUT_DEVICE_NAME";
    public static final String APP_PROPERTY_INPUT_DEVICE_NAME = "speech.audio.input-device-name";

    private SpeechAudioConfig() {
    }

    public static Optional<String> resolveInputDeviceName() {
        String configured = firstNonBlank(
                System.getProperty(SYSTEM_PROPERTY_INPUT_DEVICE_NAME),
                System.getenv(ENV_INPUT_DEVICE_NAME),
                AppConfig.load().properties().getProperty(APP_PROPERTY_INPUT_DEVICE_NAME));
        return Optional.ofNullable(configured);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}