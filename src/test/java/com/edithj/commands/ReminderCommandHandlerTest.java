package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderRepository;
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

    private static final class InMemoryReminderRepository implements ReminderRepository {

        private final List<Reminder> reminders = new ArrayList<>();

        void setReminders(List<Reminder> values) {
            reminders.clear();
            reminders.addAll(values);
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
