package com.edithj.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class AppConfig {

    private static final String APPLICATION_PROPERTIES = "/application.properties";
    private static final String LOCAL_OVERRIDE_FILE = "edith.properties";

    private final EnvConfig envConfig;
    private final Properties properties;
    private final ModelConfig modelConfig;
    private final StorageConfig storageConfig;

    private AppConfig(EnvConfig envConfig, Properties properties) {
        this.envConfig = Objects.requireNonNull(envConfig, "envConfig");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.modelConfig = ModelConfig.load(this.envConfig, this.properties);
        this.storageConfig = StorageConfig.load(this.envConfig, this.properties);
    }

    public static AppConfig load() {
        EnvConfig envConfig = EnvConfig.system();
        Properties properties = loadProperties();
        return new AppConfig(envConfig, properties);
    }

    public EnvConfig envConfig() {
        return envConfig;
    }

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
        return properties.getProperty("app.name", "EDITH-J");
    }

    public String systemPromptPath() {
        return properties.getProperty("app.system-prompt", "/prompts/system-prompt.txt");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = AppConfig.class.getResourceAsStream(APPLICATION_PROPERTIES)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load application properties", exception);
        }

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
