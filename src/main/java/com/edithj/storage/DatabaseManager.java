package com.edithj.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final Path databasePath;
    private final String jdbcUrl;

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    public static DatabaseManager defaultManager() {
        return new DatabaseManager(StoragePaths.databasePath());
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public synchronized void initialize() {
        try {
            Files.createDirectories(databasePath.toAbsolutePath().getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create storage directory", exception);
        }

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS notes (
                        id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id TEXT PRIMARY KEY,
                        reminder_text TEXT NOT NULL,
                        due_at TEXT,
                        is_completed INTEGER NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS memory_entries (
                        id TEXT PRIMARY KEY,
                        category TEXT NOT NULL,
                        content TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS app_meta (
                        meta_key TEXT PRIMARY KEY,
                        meta_value TEXT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS chat_history (
                        id TEXT PRIMARY KEY,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp TEXT NOT NULL
                    )
                    """);
            ensureColumn(statement, "chat_history", "source", "TEXT");
            ensureColumn(statement, "chat_history", "intent_type", "TEXT");
            ensureColumn(statement, "chat_history", "success", "INTEGER NOT NULL DEFAULT 1");
            ensureColumn(statement, "chat_history", "requires_approval", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(statement, "chat_history", "approval_type", "TEXT");
            ensureColumn(statement, "chat_history", "explanation", "TEXT");
            ensureColumn(statement, "chat_history", "plan_goal", "TEXT");
            ensureColumn(statement, "chat_history", "task_plan_json", "TEXT");
            ensureColumn(statement, "chat_history", "actions_json", "TEXT");
            ensureColumn(statement, "chat_history", "recovery_options_json", "TEXT");
            ensureColumn(statement, "chat_history", "metadata_json", "TEXT");
            logger.debug("SQLite schema initialized at {}", databasePath);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize SQLite schema", exception);
        }
    }

    private void ensureColumn(Statement statement, String tableName, String columnName, String definition) throws SQLException {
        try {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        } catch (SQLException exception) {
            String message = exception.getMessage();
            if (message == null || !message.toLowerCase().contains("duplicate column name")) {
                throw exception;
            }
        }
    }
}
