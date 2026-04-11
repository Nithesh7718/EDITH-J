package com.edithj.config;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EnvConfig {

    private final Map<String, String> environment;

    private EnvConfig(Map<String, String> environment) {
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
    }

    public static EnvConfig system() {
        return new EnvConfig(System.getenv());
    }

    public Optional<String> get(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        String value = environment.get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value.trim());
    }

    public String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public boolean isPresent(String key) {
        return get(key).isPresent();
    }
}
