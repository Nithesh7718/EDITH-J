package com.edithj.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.notes.FileNoteRepository;
import com.edithj.notes.Note;
import com.edithj.notes.SQLiteNoteRepository;
import com.edithj.reminders.FileReminderRepository;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.SQLiteReminderRepository;

public class JsonToSqliteMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(JsonToSqliteMigrationService.class);
    private static final String MIGRATION_KEY = "json_to_sqlite_v1_done";

    private final DatabaseManager databaseManager;
    private final FileNoteRepository fileNoteRepository;
    private final FileReminderRepository fileReminderRepository;
    private final SQLiteNoteRepository sqliteNoteRepository;
    private final SQLiteReminderRepository sqliteReminderRepository;

    public JsonToSqliteMigrationService(DatabaseManager databaseManager) {
        this(databaseManager,
                new FileNoteRepository(),
                new FileReminderRepository(),
                new SQLiteNoteRepository(databaseManager),
                new SQLiteReminderRepository(databaseManager));
    }

    JsonToSqliteMigrationService(DatabaseManager databaseManager,
            FileNoteRepository fileNoteRepository,
            FileReminderRepository fileReminderRepository,
            SQLiteNoteRepository sqliteNoteRepository,
            SQLiteReminderRepository sqliteReminderRepository) {
        this.databaseManager = databaseManager;
        this.fileNoteRepository = fileNoteRepository;
        this.fileReminderRepository = fileReminderRepository;
        this.sqliteNoteRepository = sqliteNoteRepository;
        this.sqliteReminderRepository = sqliteReminderRepository;
    }

    public synchronized void migrateOnce() {
        databaseManager.initialize();

        if (isMarkedDone()) {
            logger.debug("JSON->SQLite migration already completed.");
            return;
        }

        try {
            List<Note> notes = fileNoteRepository.findAll();
            List<Reminder> reminders = fileReminderRepository.findAll();

            int migratedNotes = 0;
            int migratedReminders = 0;

            for (Note note : notes) {
                if (sqliteNoteRepository.findById(note.getId()).isEmpty()) {
                    sqliteNoteRepository.save(note);
                    migratedNotes++;
                }
            }

            for (Reminder reminder : reminders) {
                if (sqliteReminderRepository.findById(reminder.getId()).isEmpty()) {
                    sqliteReminderRepository.save(reminder);
                    migratedReminders++;
                }
            }

            markDone();
            logger.info("JSON->SQLite migration complete. notes={}, reminders={}", migratedNotes, migratedReminders);
        } catch (RuntimeException exception) {
            logger.warn("JSON->SQLite migration failed; app will continue with repository fallback.", exception);
        }
    }

    private boolean isMarkedDone() {
        String sql = "SELECT meta_value FROM app_meta WHERE meta_key = ?";
        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, MIGRATION_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && "true".equalsIgnoreCase(resultSet.getString("meta_value"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read migration marker", exception);
        }
    }

    private void markDone() {
        String sql = """
                INSERT INTO app_meta(meta_key, meta_value)
                VALUES(?, ?)
                ON CONFLICT(meta_key) DO UPDATE SET meta_value = excluded.meta_value
                """;

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, MIGRATION_KEY);
            statement.setString(2, "true");
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to write migration marker", exception);
        }
    }
}
