package com.edithj.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.edithj.notes.FileNoteRepository;
import com.edithj.notes.Note;
import com.edithj.notes.SQLiteNoteRepository;
import com.edithj.reminders.FileReminderRepository;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.SQLiteReminderRepository;

class JsonToSqliteMigrationServiceTest {

    @Test
    void migrateOnce_copiesJsonDataAndDoesNotDuplicate() throws Exception {
        Path dbPath = Files.createTempFile("edithj-migrate", ".db");
        Path notesJson = Files.createTempFile("edithj-notes", ".json");
        Path remindersJson = Files.createTempFile("edithj-reminders", ".json");

        dbPath.toFile().deleteOnExit();
        notesJson.toFile().deleteOnExit();
        remindersJson.toFile().deleteOnExit();
        Files.writeString(notesJson, "[]");
        Files.writeString(remindersJson, "[]");

        DatabaseManager manager = new DatabaseManager(dbPath);
        manager.initialize();

        FileNoteRepository fileNoteRepository = new FileNoteRepository(notesJson);
        FileReminderRepository fileReminderRepository = new FileReminderRepository(remindersJson);
        SQLiteNoteRepository sqliteNoteRepository = new SQLiteNoteRepository(manager);
        SQLiteReminderRepository sqliteReminderRepository = new SQLiteReminderRepository(manager);

        Note note = new Note("note-1", "Title", "Body", Instant.now(), Instant.now());
        Reminder reminder = new Reminder("rem-1", "Call mom", Instant.now().plusSeconds(60), false, Instant.now(), Instant.now());
        fileNoteRepository.save(note);
        fileReminderRepository.save(reminder);

        JsonToSqliteMigrationService migrationService = new JsonToSqliteMigrationService(
                manager,
                fileNoteRepository,
                fileReminderRepository,
                sqliteNoteRepository,
                sqliteReminderRepository);

        migrationService.migrateOnce();
        migrationService.migrateOnce();

        assertEquals(1, sqliteNoteRepository.findAll().size());
        assertEquals(1, sqliteReminderRepository.findAll().size());
        assertTrue(sqliteNoteRepository.findById("note-1").isPresent());
        assertTrue(sqliteReminderRepository.findById("rem-1").isPresent());
    }
}
