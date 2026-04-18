package com.edithj.storage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StoragePathsTest {

    @Test
    void dataDirectory_returnsValidPath() {
        Path path = StoragePaths.dataDirectory();
        assertNotNull(path);
        assertTrue(path.toString().contains(".edith-j"));
    }

    @Test
    void dataDirectory_returnsConsistentPath() {
        Path path1 = StoragePaths.dataDirectory();
        Path path2 = StoragePaths.dataDirectory();
        assertTrue(path1.equals(path2));
    }

    @Test
    void notesPath_returnsValidPath() {
        Path path = StoragePaths.notesPath();
        assertNotNull(path);
        assertTrue(path.toString().contains("notes"));
        assertTrue(path.toString().contains(".json"));
    }

    @Test
    void notesPath_returnsConsistentPath() {
        Path path1 = StoragePaths.notesPath();
        Path path2 = StoragePaths.notesPath();
        assertTrue(path1.equals(path2));
    }

    @Test
    void remindersPath_returnsValidPath() {
        Path path = StoragePaths.remindersPath();
        assertNotNull(path);
        assertTrue(path.toString().contains("reminders"));
        assertTrue(path.toString().contains(".json"));
    }

    @Test
    void remindersPath_returnsConsistentPath() {
        Path path1 = StoragePaths.remindersPath();
        Path path2 = StoragePaths.remindersPath();
        assertTrue(path1.equals(path2));
    }

    @Test
    void logsDirectory_returnsValidPath() {
        Path path = StoragePaths.logsDirectory();
        assertNotNull(path);
        assertTrue(path.toString().contains("logs") || path.toString().contains(".edith-j"));
    }

    @Test
    void allPaths_useDataDirectory() {
        Path dataDir = StoragePaths.dataDirectory();
        Path notesPath = StoragePaths.notesPath();
        Path remindersPath = StoragePaths.remindersPath();
        
        assertTrue(notesPath.toString().startsWith(dataDir.toString()));
        assertTrue(remindersPath.toString().startsWith(dataDir.toString()));
    }
}

