package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderRepository;
import com.edithj.reminders.ReminderService;

class ReminderCommandHandlerTest {

    @Test
    void handle_listCommandFormatsReminders() {
        ReminderRepository reminderRepository = mock(ReminderRepository.class);
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        Reminder reminder = new Reminder("12345678-abcd", "Pay bill", Instant.now().plusSeconds(60), false, Instant.now(), Instant.now());
        when(reminderRepository.findAll()).thenReturn(List.of(reminder));

        String response = handler.handle(new CommandHandler.CommandContext("list reminders", "list", "typed"));

        assertTrue(response.contains("[PENDING]"));
        assertTrue(response.contains("Pay bill"));
    }

    @Test
    void handle_markDoneUsesService() {
        ReminderRepository reminderRepository = mock(ReminderRepository.class);
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        Reminder reminder = new Reminder("id-3", "Send mail", Instant.now(), false, Instant.now(), Instant.now());
        when(reminderRepository.findById("id-3")).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(reminder)).thenReturn(reminder);

        String response = handler.handle(new CommandHandler.CommandContext("done", "done id-3", "typed"));

        assertTrue(response.contains("marked as done"));
    }

    @Test
    void handle_snoozeMissingReminderReturnsMessage() {
        ReminderRepository reminderRepository = mock(ReminderRepository.class);
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        when(reminderRepository.findById("id-4")).thenReturn(Optional.empty());

        String response = handler.handle(new CommandHandler.CommandContext("snooze", "snooze id-4 10 minutes", "typed"));

        assertTrue(response.contains("Could not find reminder with ID: id-4"));
    }

    @Test
    void handle_createReminderRequiresTimeHint() {
        ReminderRepository reminderRepository = mock(ReminderRepository.class);
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        String response = handler.handle(new CommandHandler.CommandContext("remind", "remind me to stretch", "typed"));

        assertTrue(response.contains("I need a time hint"));
    }

    @Test
    void handle_createReminderSuccess() {
        ReminderRepository reminderRepository = mock(ReminderRepository.class);
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        when(reminderRepository.save(org.mockito.ArgumentMatchers.any(Reminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String response = handler.handle(new CommandHandler.CommandContext("remind", "remind me to stretch in 5 minutes", "typed"));

        assertTrue(response.startsWith("Reminder set!"));
    }
}
