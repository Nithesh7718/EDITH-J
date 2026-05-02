package com.edithj.storage;

import java.nio.file.Path;

import com.edithj.config.AppPaths;

/**
 * Centralized storage path management for application data.
 */
public final class StoragePaths {

    private static final Path DATA_DIR = AppPaths.dataDirectory();

    private StoragePaths() {
        // Utility class
    }

    public static Path notesPath() {
        return DATA_DIR.resolve("notes.json");
    }

    public static Path remindersPath() {
        return DATA_DIR.resolve("reminders.json");
    }

    public static Path databasePath() {
        return DATA_DIR.resolve("edith.db");
    }

    public static Path logsDirectory() {
        return AppPaths.logsDirectory();
    }

    public static Path dataDirectory() {
        return DATA_DIR;
    }
}
