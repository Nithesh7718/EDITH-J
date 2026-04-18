package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.edithj.launcher.FakeLauncher;
import com.edithj.notes.Note;
import com.edithj.notes.NoteRepository;
import com.edithj.notes.NoteService;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderRepository;
import com.edithj.reminders.ReminderService;

class DesktopToolsCommandHandlerTest {

    @Test
    void handle_withScreenshotReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false);
        String response = handler.handle(new CommandHandler.CommandContext("screenshot", "screenshot", "typed"));

        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withClipboardReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false);
        String response = handler.handle(new CommandHandler.CommandContext("clipboard", "clipboard", "typed"));

        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withoutPayloadReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false);
        String response = handler.handle(new CommandHandler.CommandContext("desktop", "", "typed"));

        assertTrue(!response.isBlank());
    }

    @Test
    void intentType_returnsDesktopTools() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false);

        assertTrue(handler.intentType().toString().contains("DESKTOP"));
    }

    @Test
    void handle_withBlankPayloadReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = newHandler(new FakeLauncher(), false);
        String response = handler.handle(new CommandHandler.CommandContext("desktop", "   ", "typed"));

        assertTrue(!response.isBlank());
    }

    @Test
    void handle_workModeDisabled_doesNotLaunchAnyTarget() {
        FakeLauncher fakeLauncher = new FakeLauncher();
        DesktopToolsCommandHandler handler = newHandler(fakeLauncher, false);

        String response = handler.handle(new CommandHandler.CommandContext("start work mode", "start work mode", "typed"));

        assertEquals("Launcher demo commands are disabled in this configuration.", response);
        assertEquals(0, fakeLauncher.launchCount());
    }

    @Test
    void handle_workModeEnabled_launchesDemoTargetsWithFakeLauncher() {
        FakeLauncher fakeLauncher = new FakeLauncher();
        DesktopToolsCommandHandler handler = newHandler(fakeLauncher, true);

        String response = handler.handle(new CommandHandler.CommandContext("start work mode", "start work mode", "typed"));

        assertTrue(response.contains("Work mode started"));
        assertEquals(4, fakeLauncher.launchCount());
        assertEquals("notepad", fakeLauncher.lastOpenedApp());
    }

    private DesktopToolsCommandHandler newHandler(FakeLauncher fakeLauncher, boolean smokeLaunchersEnabled) {
        NoteService noteService = new NoteService(new InMemoryNoteRepository());
        ReminderService reminderService = new ReminderService(new InMemoryReminderRepository());
        return new DesktopToolsCommandHandler(fakeLauncher, noteService, reminderService, smokeLaunchersEnabled);
    }

    private static final class InMemoryNoteRepository implements NoteRepository {

        private final List<Note> notes = new ArrayList<>();

        @Override
        public List<Note> findAll() {
            return new ArrayList<>(notes);
        }

        @Override
        public Optional<Note> findById(String noteId) {
            return notes.stream().filter(note -> note.getId().equals(noteId)).findFirst();
        }

        @Override
        public Note save(Note note) {
            notes.removeIf(existing -> existing.getId().equals(note.getId()));
            notes.add(note);
            return note;
        }

        @Override
        public boolean deleteById(String noteId) {
            return notes.removeIf(existing -> existing.getId().equals(noteId));
        }
    }

    private static final class InMemoryReminderRepository implements ReminderRepository {

        private final List<Reminder> reminders = new ArrayList<>();

        InMemoryReminderRepository() {
            reminders.add(new Reminder("r-1", "test reminder", Instant.now().plusSeconds(600), false, Instant.now(), Instant.now()));
        }

        @Override
        public List<Reminder> findAll() {
            return new ArrayList<>(reminders);
        }

        @Override
        public Optional<Reminder> findById(String reminderId) {
            return reminders.stream().filter(reminder -> reminder.getId().equals(reminderId)).findFirst();
        }

        @Override
        public Reminder save(Reminder reminder) {
            reminders.removeIf(existing -> existing.getId().equals(reminder.getId()));
            reminders.add(reminder);
            return reminder;
        }

        @Override
        public boolean deleteById(String reminderId) {
            return reminders.removeIf(existing -> existing.getId().equals(reminderId));
        }
    }
}
