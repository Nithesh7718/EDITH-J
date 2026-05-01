package com.edithj.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.edithj.assistant.AssistantResponse;
import com.edithj.chat.ChatMessage;
import com.edithj.chat.ConversationHistoryService;
import com.edithj.chat.SQLiteChatRepository;
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

    @Test
    void sqliteChatRepository_persistsStructuredAssistantCards() throws Exception {
        Path dbPath = Files.createTempFile("edithj-chat", ".db");
        dbPath.toFile().deleteOnExit();

        DatabaseManager manager = new DatabaseManager(dbPath);
        manager.initialize();
        SQLiteChatRepository repository = new SQLiteChatRepository(manager);
        ConversationHistoryService historyService = new ConversationHistoryService(repository);

        AssistantResponse response = new AssistantResponse(
                com.edithj.assistant.IntentType.GENERAL_CHAT,
                "create a reminder, email, and calendar event",
                "I broke this into a task plan so we can execute it step by step.",
                "typed",
                "Planner",
                true,
                false,
                "",
                "This request spans multiple actions, so I prepared a structured plan first.",
                new AssistantResponse.TaskPlan("Follow up on launch prep", List.of(
                        new AssistantResponse.TaskPlanStep("step-1", "Create reminder", "Reminder tool", "pending", "Save a reminder."),
                        new AssistantResponse.TaskPlanStep("step-2", "Draft email", "Email tool", "pending", "Prepare a draft."))),
                List.of(new AssistantResponse.AssistantAction("execute-plan", "Start plan", "plan", "start this plan")),
                List.of(new AssistantResponse.RecoveryOption("Refine plan", "refine this plan")),
                Map.of("planner", "deterministic"));

        historyService.appendAssistantResponse(response);

        List<ChatMessage> history = repository.findRecent(10);
        assertEquals(1, history.size());

        ChatMessage stored = history.get(0);
        assertEquals("Planner", stored.source());
        assertEquals("Follow up on launch prep", stored.planGoal());
        assertNotNull(stored.taskPlan());
        assertEquals(2, stored.taskPlan().steps().size());
        assertEquals("Start plan", stored.actions().get(0).label());
        assertEquals("Refine plan", stored.recoveryOptions().get(0).label());
        assertEquals("deterministic", stored.metadata().get("planner"));
        assertFalse(stored.requiresApproval());
    }
}
