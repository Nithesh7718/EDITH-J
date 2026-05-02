package com.edithj.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.resilience.FailureType;
import com.edithj.resilience.HealthMonitorRegistry;
import com.edithj.resilience.HealthSignal;
import com.edithj.resilience.IncidentSeverity;

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
        int attempt = 0;
        while (true) {
            try {
                return DriverManager.getConnection(jdbcUrl);
            } catch (SQLException exception) {
                if (attempt < 3 && isSqliteBusy(exception)) {
                    attempt++;
                    HealthMonitorRegistry.instance().registerHealthSignal(new HealthSignal(
                            "storage",
                            FailureType.STORAGE,
                            IncidentSeverity.MEDIUM,
                            "SQLite database is locked. Retry attempt " + attempt + ".",
                            Instant.now(),
                            Map.of("attempt", String.valueOf(attempt), "jdbcUrl", jdbcUrl)));
                    try {
                        pauseBeforeRetry(attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrupted while retrying SQLite connection", interrupted);
                    }
                    continue;
                }
                throw exception;
            }
        }
    }

    private void pauseBeforeRetry(int attempt) throws InterruptedException {
        long delayMillis = 150L * attempt;
        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }
    }

    private boolean isSqliteBusy(SQLException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        return message.contains("database is locked")
                || message.contains("busy")
                || message.contains("timeout");
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

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS health_snapshots (
                        component_id TEXT PRIMARY KEY,
                        status TEXT NOT NULL,
                        health_score REAL NOT NULL,
                        health_mode TEXT NOT NULL,
                        last_failure_at TEXT,
                        last_success_at TEXT,
                        current_incident_id TEXT
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS incidents (
                        id TEXT PRIMARY KEY,
                        component_id TEXT NOT NULL,
                        failure_type TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        message TEXT NOT NULL,
                        diagnosis TEXT,
                        created_at TEXT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS recovery_attempts (
                        id TEXT PRIMARY KEY,
                        incident_id TEXT NOT NULL,
                        plan_id TEXT NOT NULL,
                        started_at TEXT NOT NULL,
                        completed_at TEXT,
                        success INTEGER NOT NULL DEFAULT 0,
                        notes TEXT
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS recovery_actions (
                        id TEXT PRIMARY KEY,
                        attempt_id TEXT NOT NULL,
                        action_type TEXT NOT NULL,
                        description TEXT NOT NULL,
                        executed_at TEXT NOT NULL,
                        result TEXT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS component_state (
                        component_id TEXT PRIMARY KEY,
                        state_payload TEXT,
                        updated_at TEXT NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS adaptive_failure_memory (
                        failure_signature TEXT PRIMARY KEY,
                        failure_count INTEGER NOT NULL,
                        last_seen_at TEXT NOT NULL,
                        recovery_hint TEXT
                    )
                    """);

            logger.debug("SQLite schema initialized at {}", databasePath);
            HealthMonitorRegistry.instance().registerHealthSignal(new HealthSignal(
                    "storage",
                    FailureType.STORAGE,
                    IncidentSeverity.LOW,
                    "SQLite schema initialization completed successfully.",
                    Instant.now(),
                    Map.of("databasePath", databasePath.toString())));
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
