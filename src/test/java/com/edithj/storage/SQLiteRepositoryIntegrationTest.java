package com.edithj.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.edithj.notes.Note;
import com.edithj.notes.SQLiteNoteRepository;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.SQLiteReminderRepository;

class SQLiteRepositoryIntegrationTest {

    @Test
    void sqliteNoteRepository_supportsCrud() throws Exception {
        Path dbPath = Files.createTempFile("edithj-note", ".db");
        dbPath.toFile().deleteOnExit();

        DatabaseManager manager = new DatabaseManager(dbPath);
        manager.initialize();
        SQLiteNoteRepository repository = new SQLiteNoteRepository(manager);

        Note created = Note.newNote("Title", "Body");
        repository.save(created);

        assertTrue(repository.findById(created.getId()).isPresent());
        assertEquals(1, repository.findAll().size());

        assertTrue(repository.deleteById(created.getId()));
        assertTrue(repository.findById(created.getId()).isEmpty());
    }

    @Test
    void sqliteReminderRepository_supportsCrud() throws Exception {
        Path dbPath = Files.createTempFile("edithj-reminder", ".db");
        dbPath.toFile().deleteOnExit();

        DatabaseManager manager = new DatabaseManager(dbPath);
        manager.initialize();
        SQLiteReminderRepository repository = new SQLiteReminderRepository(manager);

        Reminder created = Reminder.newReminder("Check email", Instant.now().plusSeconds(300));
        repository.save(created);

        assertTrue(repository.findById(created.getId()).isPresent());
        assertEquals(1, repository.findAll().size());

        assertTrue(repository.deleteById(created.getId()));
        assertTrue(repository.findById(created.getId()).isEmpty());
    }
}
