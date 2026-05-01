package com.edithj.commands;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.reminders.InMemoryReminderRepository;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderService;

class ReminderCommandHandlerTest {

    @Test
    void handle_listCommandFormatsReminders() {
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        Reminder reminder = new Reminder("12345678-abcd", "Pay bill", Instant.now().plusSeconds(60), false, Instant.now(), Instant.now());
        reminderRepository.setReminders(List.of(reminder));

        String response = handler.handle(new CommandHandler.CommandContext("list reminders", "list", "typed"));

        assertTrue(response.contains("[PENDING]"));
        assertTrue(response.contains("Pay bill"));
    }

    @Test
    void handle_markDoneUsesService() {
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        Reminder reminder = new Reminder("id-3", "Send mail", Instant.now(), false, Instant.now(), Instant.now());
        reminderRepository.setReminders(List.of(reminder));

        String response = handler.handle(new CommandHandler.CommandContext("done", "done id-3", "typed"));

        assertTrue(response.contains("marked as done"));
    }

    @Test
    void handle_snoozeMissingReminderReturnsMessage() {
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        reminderRepository.setReminders(List.of());

        String response = handler.handle(new CommandHandler.CommandContext("snooze", "snooze id-4 10 minutes", "typed"));

        assertTrue(response.contains("Could not find reminder with ID: id-4"));
    }

    @Test
    void handle_createReminderRequiresTimeHint() {
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));
        String response = handler.handle(new CommandHandler.CommandContext("remind", "remind me to stretch", "typed"));

        assertTrue(response.contains("I need a time hint"));
    }

    @Test
    void handle_createReminderSuccess() {
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));

        String response = handler.handle(new CommandHandler.CommandContext("remind", "remind me to stretch in 5 minutes", "typed"));

        assertTrue(response.startsWith("Reminder set!"));
    }

    @Test
    void handle_timerWithInvalidDurationReturnsHelp() {
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));

        String response = handler.handle(new CommandHandler.CommandContext("timer", "timer later", "typed"));

        assertTrue(response.contains("Use timer like"));
    }

    @Test
    void handle_doneWithoutIdReturnsGuidance() {
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));

        String response = handler.handle(new CommandHandler.CommandContext("done", "done", "typed"));

        assertTrue(response.contains("Please provide the reminder ID"));
    }

    @Test
    void handle_reminderWithInvalidTimeFormatReturnsGuidance() {
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        ReminderCommandHandler handler = new ReminderCommandHandler(new ReminderService(reminderRepository));

        String response = handler.handle(new CommandHandler.CommandContext("remind", "remind me to call mom at 99:99", "typed"));

        assertTrue(response.contains("couldn't parse the time"));
    }
}
