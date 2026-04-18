package com.edithj.reminders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ReminderServiceTest {

    @Test
    void createReminder_savesParsedReminder() {
        InMemoryReminderRepository repository = new InMemoryReminderRepository();
        ReminderService reminderService = new ReminderService(repository);

        Reminder reminder = reminderService.createReminder("Call mom", "in 10 minutes");

        assertEquals("Call mom", reminder.getText());
        assertNotNull(reminder.getDueAt());
        assertEquals(1, repository.savedReminders().size());
    }

    @Test
    void listReminders_filtersCompletedAndSortsByDueDate() {
        InMemoryReminderRepository repository = new InMemoryReminderRepository();
        ReminderService reminderService = new ReminderService(repository);
        Reminder done = new Reminder("1", "done", Instant.now().plusSeconds(60), true, Instant.now(), Instant.now());
        Reminder early = new Reminder("2", "early", Instant.now().plusSeconds(120), false, Instant.now(), Instant.now());
        Reminder late = new Reminder("3", "late", Instant.now().plusSeconds(240), false, Instant.now(), Instant.now());
        repository.setReminders(List.of(late, done, early));

        List<Reminder> reminders = reminderService.listReminders();

        assertEquals(List.of("2", "3"), reminders.stream().map(Reminder::getId).toList());
    }

    @Test
    void markDone_updatesExistingReminder() {
        InMemoryReminderRepository repository = new InMemoryReminderRepository();
        ReminderService reminderService = new ReminderService(repository);
        Reminder reminder = new Reminder("abc123", "task", Instant.now().plusSeconds(60), false, Instant.now(), Instant.now());
        repository.setReminders(List.of(reminder));

        Optional<Reminder> updated = reminderService.markDone("abc123");

        assertTrue(updated.isPresent());
        assertTrue(updated.get().isCompleted());
    }

    @Test
    void snoozeReminder_movesDueDateForward() {
        InMemoryReminderRepository repository = new InMemoryReminderRepository();
        ReminderService reminderService = new ReminderService(repository);
        Instant base = Instant.now().plusSeconds(60);
        Reminder reminder = new Reminder("xyz789", "meeting", base, true, Instant.now(), Instant.now());
        repository.setReminders(List.of(reminder));

        Optional<Reminder> snoozed = reminderService.snoozeReminder("xyz789", Duration.ofMinutes(15));

        assertTrue(snoozed.isPresent());
        assertFalse(snoozed.get().isCompleted());
        assertTrue(snoozed.get().getDueAt().isAfter(base));
    }

    @Test
    void deleteReminder_rejectsInvalidIds() {
        InMemoryReminderRepository repository = new InMemoryReminderRepository();
        ReminderService reminderService = new ReminderService(repository);
        repository.setReminders(List.of(new Reminder("id-1", "task", Instant.now(), false, Instant.now(), Instant.now())));

        assertTrue(reminderService.deleteReminder("id-1"));
        assertFalse(reminderService.deleteReminder(""));
    }

    private static final class InMemoryReminderRepository implements ReminderRepository {

        private final List<Reminder> reminders = new ArrayList<>();
        private final List<Reminder> savedReminders = new ArrayList<>();

        void setReminders(List<Reminder> values) {
            reminders.clear();
            reminders.addAll(values);
        }

        List<Reminder> savedReminders() {
            return savedReminders;
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
            savedReminders.add(reminder);
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
