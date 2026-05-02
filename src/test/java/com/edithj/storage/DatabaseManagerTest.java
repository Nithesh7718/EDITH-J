package com.edithj.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

    @Test
    void initialize_createsNotesAndRemindersTables() throws Exception {
        Path dbPath = Files.createTempFile("edithj-db-manager", ".db");
        dbPath.toFile().deleteOnExit();

        DatabaseManager manager = new DatabaseManager(dbPath);
        manager.initialize();

        try (Connection connection = manager.openConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet notes = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='notes'")) {
                assertTrue(notes.next());
            }
            try (ResultSet reminders = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='reminders'")) {
                assertTrue(reminders.next());
            }
            try (ResultSet columns = statement.executeQuery("PRAGMA table_info(chat_history)")) {
                java.util.Set<String> columnNames = new java.util.HashSet<>();
                while (columns.next()) {
                    columnNames.add(columns.getString("name"));
                }
                assertTrue(columnNames.contains("task_plan_json"));
                assertTrue(columnNames.contains("actions_json"));
                assertTrue(columnNames.contains("recovery_options_json"));
                assertTrue(columnNames.contains("metadata_json"));
            }
        }
    }
}
