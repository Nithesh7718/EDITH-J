package com.edithj.config;

import java.nio.file.Path;
import java.util.Properties;

import com.edithj.storage.StoragePaths;

public final class StorageConfig {

    private static final String DEFAULT_BACKEND = "sqlite";

    private final String backend;
    private final Path databasePath;

    private StorageConfig(String backend, Path databasePath) {
        this.backend = backend;
        this.databasePath = databasePath;
    }

    public static StorageConfig load(EnvConfig envConfig, Properties properties) {
        String configuredBackend = envConfig.get("EDITH_STORAGE_BACKEND")
                .orElseGet(() -> properties.getProperty("storage.backend", DEFAULT_BACKEND));

        String configuredDbPath = envConfig.get("EDITH_DB_PATH")
                .orElseGet(() -> properties.getProperty("storage.db-path", StoragePaths.databasePath().toString()));

        String backend = configuredBackend == null || configuredBackend.isBlank()
                ? DEFAULT_BACKEND
                : configuredBackend.trim().toLowerCase();

        Path databasePath = Path.of(configuredDbPath == null || configuredDbPath.isBlank()
                ? StoragePaths.databasePath().toString()
                : configuredDbPath.trim());

        return new StorageConfig(backend, databasePath);
    }

    public String backend() {
        return backend;
    }

    public Path databasePath() {
        return databasePath;
    }
}
