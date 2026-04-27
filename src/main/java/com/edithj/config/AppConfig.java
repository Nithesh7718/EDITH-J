package com.edithj.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public final class AppConfig {

    private static final String LOCAL_OVERRIDE_FILE = "edith.properties";

    // Hardcoded Defaults
    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("app.name", "EDITH-J"),
            Map.entry("app.system-prompt", "/prompts/system-prompt.txt"),
            Map.entry("edith.ai.provider", "groq"),
            Map.entry("edith.automation.musicUrl", "https://music.youtube.com"),
            Map.entry("groq.base-url", "https://api.groq.com/openai/v1"),
            Map.entry("groq.model", "llama-3.3-70b-versatile"),
            Map.entry("groq.timeout-seconds", "30"),
            Map.entry("groq.temperature", "0.2"),
            Map.entry("worldmonitor.base-url", "https://www.worldmonitor.app"),
            Map.entry("storage.backend", "sqlite"),
            Map.entry("edith.dev.smokeLaunchersEnabled", "false"),
            Map.entry("edith.desktop.fileOpenEnabled", "true"),
            Map.entry("edith.desktop.clipboardWriteEnabled", "true"),
            Map.entry("speech.vosk.model-path", "models/vosk-model-small-en-us-0.15")
    );

    private static AppConfig INSTANCE;

    private final EnvConfig envConfig;
    private final Properties properties;
    private final ModelConfig modelConfig;
    private final StorageConfig storageConfig;

    private AppConfig(EnvConfig envConfig, Properties properties) {
        this.envConfig = Objects.requireNonNull(envConfig, "envConfig");
        this.properties = Objects.requireNonNull(properties, "properties");
        // Use static resolution to avoid circular dependency on load()
        this.modelConfig = ModelConfig.load(this.envConfig, this.properties);
        this.storageConfig = StorageConfig.load(this.envConfig, this.properties);
    }

    public static synchronized AppConfig load() {
        if (INSTANCE == null) {
            EnvConfig envConfig = EnvConfig.system();
            Properties properties = loadProperties();
            INSTANCE = new AppConfig(envConfig, properties);
        }
        return INSTANCE;
    }

    public EnvConfig envConfig() {
        return envConfig;
    }

    /**
     * @return a COPY of the loaded properties.
     */
    public Properties properties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    public ModelConfig modelConfig() {
        return modelConfig;
    }

    public StorageConfig storageConfig() {
        return storageConfig;
    }

    public String appName() {
        return get("app.name", DEFAULTS.get("app.name"));
    }

    public String systemPromptPath() {
        return get("app.system-prompt", DEFAULTS.get("app.system-prompt"));
    }

    public boolean isDevSmokeLaunchersEnabled() {
        return Boolean.parseBoolean(get("edith.dev.smokeLaunchersEnabled", DEFAULTS.get("edith.dev.smokeLaunchersEnabled")));
    }

    public boolean isDesktopFileOpenEnabled() {
        return Boolean.parseBoolean(get("edith.desktop.fileOpenEnabled", DEFAULTS.get("edith.desktop.fileOpenEnabled")));
    }

    public boolean isDesktopClipboardWriteEnabled() {
        return Boolean.parseBoolean(get("edith.desktop.clipboardWriteEnabled", DEFAULTS.get("edith.desktop.clipboardWriteEnabled")));
    }

    /**
     * Resolves a configuration value with precedence:
     * 1. Environment Variable (normalized key, e.g. "edith.ai.provider" -> "EDITH_AI_PROVIDER")
     * 2. Properties file (edith.properties)
     * 3. Provided default value.
     */
    public String get(String key, String defaultValue) {
        return resolve(this.envConfig, this.properties, key, defaultValue);
    }

    /**
     * Static version of resolve to avoid circular dependencies during startup.
     */
    public static String resolve(EnvConfig envConfig, Properties properties, String key, String defaultValue) {
        // 1. Check Env
        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envVal = envConfig.get(envKey).orElse(null);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }

        // 2. Check properties
        if (properties != null) {
            String propVal = properties.getProperty(key);
            if (propVal != null && !propVal.isBlank()) {
                return propVal.trim();
            }
        }

        return defaultValue;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        // ONLY load from edith.properties in project root.
        Path localOverride = Path.of(LOCAL_OVERRIDE_FILE);
        if (Files.isRegularFile(localOverride)) {
            try (InputStream inputStream = Files.newInputStream(localOverride)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load local configuration from " + LOCAL_OVERRIDE_FILE, exception);
            }
        }

        return properties;
    }
}
