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
        String backend = AppConfig.resolve(envConfig, properties, "storage.backend", DEFAULT_BACKEND).trim().toLowerCase();
        String dbPath = AppConfig.resolve(envConfig, properties, "storage.db-path", StoragePaths.databasePath().toString());

        return new StorageConfig(backend, Path.of(dbPath.trim()));
    }

    public String backend() {
        return backend;
    }

    public Path databasePath() {
        return databasePath;
    }
}
