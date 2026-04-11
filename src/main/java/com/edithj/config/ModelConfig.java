package com.edithj.config;

import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

public final class ModelConfig {

    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration requestTimeout;
    private final double temperature;

    private ModelConfig(String apiKey, String baseUrl, String model, Duration requestTimeout, double temperature) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = normalizeValue(model, DEFAULT_MODEL);
        this.requestTimeout = requestTimeout == null ? DEFAULT_TIMEOUT : requestTimeout;
        this.temperature = clampTemperature(temperature);
    }

    public static ModelConfig load(EnvConfig envConfig, Properties properties) {
        Objects.requireNonNull(envConfig, "envConfig");
        Properties safeProperties = properties == null ? new Properties() : properties;

        String apiKey = envConfig.get("GROQ_API_KEY").orElse("");
        String baseUrl = envConfig.get("GROQ_BASE_URL")
                .orElseGet(() -> safeProperties.getProperty("groq.base-url", DEFAULT_BASE_URL));
        String model = envConfig.get("GROQ_MODEL")
                .orElseGet(() -> safeProperties.getProperty("groq.model", DEFAULT_MODEL));

        long timeoutSeconds = parseLong(
                envConfig.get("GROQ_TIMEOUT_SECONDS")
                        .orElse(safeProperties.getProperty("groq.timeout-seconds", "30")),
                DEFAULT_TIMEOUT.getSeconds());
        double temperature = parseDouble(
                envConfig.get("GROQ_TEMPERATURE")
                        .orElse(safeProperties.getProperty("groq.temperature", "0.2")),
                0.2d);

        return new ModelConfig(apiKey, baseUrl, model, Duration.ofSeconds(timeoutSeconds), temperature);
    }

    public static ModelConfig load() {
        return AppConfig.load().modelConfig();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String apiKey() {
        return apiKey;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String model() {
        return model;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public double temperature() {
        return temperature;
    }

    public String chatCompletionsUrl() {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }

        return baseUrl + "/chat/completions";
    }

    public String missingApiKeyMessage() {
        return "Groq is not configured. Set the GROQ_API_KEY environment variable to enable AI replies.";
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String value = normalizeValue(baseUrl, DEFAULT_BASE_URL);
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String normalizeValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException exception) {
            return defaultValue;
        }
    }

    private static double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (RuntimeException exception) {
            return defaultValue;
        }
    }

    private static double clampTemperature(double temperature) {
        if (Double.isNaN(temperature) || Double.isInfinite(temperature)) {
            return 0.2d;
        }

        if (temperature < 0.0d) {
            return 0.0d;
        }

        if (temperature > 2.0d) {
            return 2.0d;
        }

        return temperature;
    }
}
