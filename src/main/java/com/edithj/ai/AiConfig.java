package com.edithj.ai;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import com.edithj.config.AppConfig;
import com.edithj.config.EnvConfig;

public final class AiConfig {

    public enum Provider {
        GROQ,
        GEMINI,
        OPENAI,
        SARVAM;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Provider from(String value) {
            if (value == null || value.isBlank()) {
                return GROQ;
            }

            String normalized = value.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "gemini" -> GEMINI;
                case "openai" -> OPENAI;
                case "sarvam" -> SARVAM;
                default -> GROQ;
            };
        }
    }

    private final EnvConfig envConfig;
    private final Properties properties;

    public AiConfig(EnvConfig envConfig, Properties properties) {
        this.envConfig = Objects.requireNonNull(envConfig, "envConfig");
        this.properties = new Properties();
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    public static AiConfig load() {
        AppConfig appConfig = AppConfig.load();
        return new AiConfig(appConfig.envConfig(), appConfig.properties());
    }

    public Provider selectedProvider() {
        return Provider.from(property("edith.ai.provider", "groq"));
    }

    public String resolveApiKey(Provider provider) {
        Provider safeProvider = provider == null ? Provider.GROQ : provider;
        String envValue = switch (safeProvider) {
            case GROQ -> envConfig.get("GROQ_API_KEY").orElse("");
            case GEMINI -> envConfig.get("GEMINI_API_KEY").orElse("");
            case OPENAI -> envConfig.get("OPENAI_API_KEY").orElse("");
            case SARVAM -> envConfig.get("SARVAM_API_KEY").orElse("");
        };

        if (!envValue.isBlank()) {
            return envValue;
        }

        return switch (safeProvider) {
            case GROQ -> property("edith.ai.groq.apiKey", "");
            case GEMINI -> property("edith.ai.gemini.apiKey", "");
            case OPENAI -> property("edith.ai.openai.apiKey", "");
            case SARVAM -> property("edith.ai.sarvam.apiKey", "");
        };
    }

    public String property(String key, String defaultValue) {
        // Use static resolution to avoid circular dependencies
        return AppConfig.resolve(this.envConfig, this.properties, key, defaultValue);
    }

    public Path workspaceDir() {
        String configured = property("edith.ai.workspaceDir", "");
        if (!configured.isBlank()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }

        return Paths.get(System.getProperty("user.home", "."), "EDITH-workspace")
                .toAbsolutePath()
                .normalize();
    }

    public String launchOverride(String appAlias) {
        if (appAlias == null || appAlias.isBlank()) {
            return "";
        }
        return property("edith.launch." + appAlias.trim().toLowerCase(Locale.ROOT), "");
    }
}
