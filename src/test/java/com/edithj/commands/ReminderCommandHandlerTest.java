package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderService;

@ExtendWith(MockitoExtension.class)
class ReminderCommandHandlerTest {

    @Mock
    private ReminderService reminderService;

    private ReminderCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReminderCommandHandler(reminderService);
    }

    @Test
    void handle_listCommandFormatsReminders() {
        Reminder reminder = new Reminder("12345678-abcd", "Pay bill", Instant.now().plusSeconds(60), false, Instant.now(), Instant.now());
        when(reminderService.listReminders()).thenReturn(List.of(reminder));

        String response = handler.handle(new CommandHandler.CommandContext("list reminders", "list", "typed"));

        assertTrue(response.contains("[PENDING]"));
        assertTrue(response.contains("Pay bill"));
    }

    @Test
    void handle_markDoneUsesService() {
        Reminder reminder = new Reminder("id-3", "Send mail", Instant.now(), true, Instant.now(), Instant.now());
        when(reminderService.markDone("id-3")).thenReturn(Optional.of(reminder));

        String response = handler.handle(new CommandHandler.CommandContext("done", "done id-3", "typed"));

        assertTrue(response.contains("marked as done"));
    }

    @Test
    void handle_snoozeMissingReminderReturnsMessage() {
        when(reminderService.snoozeReminder(org.mockito.ArgumentMatchers.eq("id-4"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        String response = handler.handle(new CommandHandler.CommandContext("snooze", "snooze id-4 10 minutes", "typed"));

        assertTrue(response.contains("Could not find reminder with ID: id-4"));
    }

    @Test
    void handle_createReminderRequiresTimeHint() {
        String response = handler.handle(new CommandHandler.CommandContext("remind", "remind me to stretch", "typed"));

        assertTrue(response.contains("I need a time hint"));
    }

    @Test
    void handle_createReminderSuccess() {
        Reminder created = new Reminder("new-7", "stretch", Instant.now().plusSeconds(300), false, Instant.now(), Instant.now());
        when(reminderService.createReminder("stretch", "in 5 minutes")).thenReturn(created);

        String response = handler.handle(new CommandHandler.CommandContext("remind", "remind me to stretch in 5 minutes", "typed"));

        assertTrue(response.startsWith("Reminder set!"));
    }
}
