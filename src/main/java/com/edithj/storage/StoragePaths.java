package com.edithj.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralized storage path management for application data.
 */
public final class StoragePaths {

    private static final Path DATA_DIR = Paths.get("src", "main", "resources", "data");

    private StoragePaths() {
        // Utility class
    }

    public static Path notesPath() {
        return DATA_DIR.resolve("notes.json");
    }

    public static Path remindersPath() {
        return DATA_DIR.resolve("reminders.json");
    }

    public static Path dataDirectory() {
        return DATA_DIR;
    }
}
