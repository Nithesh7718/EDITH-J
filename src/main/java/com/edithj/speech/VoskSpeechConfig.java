package com.edithj.speech;

import java.nio.file.Path;
import java.util.Objects;

import com.edithj.config.AppConfig;

public final class VoskSpeechConfig {

    public static final String SYSTEM_PROPERTY_MODEL_PATH = "edithj.vosk.modelPath";
    public static final String ENV_MODEL_PATH = "EDITHJ_VOSK_MODEL_PATH";
    public static final String APP_PROPERTY_MODEL_PATH = "speech.vosk.model-path";
    public static final String DEFAULT_MODEL_PATH = "models/vosk-model";

    private VoskSpeechConfig() {
    }

    public static Path resolveModelPath() {
        String configured = firstNonBlank(
                System.getProperty(SYSTEM_PROPERTY_MODEL_PATH),
                System.getenv(ENV_MODEL_PATH),
                AppConfig.load().properties().getProperty(APP_PROPERTY_MODEL_PATH),
                DEFAULT_MODEL_PATH);
        return Path.of(Objects.requireNonNull(configured, "configured")).toAbsolutePath().normalize();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return DEFAULT_MODEL_PATH;
    }
}
