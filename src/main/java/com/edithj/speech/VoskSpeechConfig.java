package com.edithj.speech;

import java.nio.file.Path;
import java.nio.file.Files;

import com.edithj.config.AppConfig;
import com.edithj.config.AppPaths;

public final class VoskSpeechConfig {

    public static final String SYSTEM_PROPERTY_MODEL_PATH = "edithj.vosk.modelPath";
    public static final String ENV_MODEL_PATH = "EDITHJ_VOSK_MODEL_PATH";
    public static final String APP_PROPERTY_MODEL_PATH = "speech.vosk.model-path";
    public static final String DEFAULT_MODEL_PATH = "models/vosk-model-small-en-us-0.15";

    private VoskSpeechConfig() {
    }

    public static Path resolveModelPath() {
        String configured = firstNonBlank(
                System.getProperty(SYSTEM_PROPERTY_MODEL_PATH),
                System.getenv(ENV_MODEL_PATH),
                AppConfig.load().get(APP_PROPERTY_MODEL_PATH, DEFAULT_MODEL_PATH));

        Path bundledModel = AppPaths.bundledModelDirectory();
        if (configured == null || configured.isBlank() || DEFAULT_MODEL_PATH.equals(configured.trim())) {
            if (Files.isDirectory(bundledModel)) {
                return bundledModel;
            }
        }

        Path installResolved = AppPaths.resolveAgainstInstallDirectory(configured);
        if (Files.isDirectory(installResolved)) {
            return installResolved;
        }

        return Path.of(configured).toAbsolutePath().normalize();
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
