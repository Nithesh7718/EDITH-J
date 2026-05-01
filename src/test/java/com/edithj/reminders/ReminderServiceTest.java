package com.edithj.reminders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ReminderServiceTest {

    @Test
    void createReminder_savesParsedReminder() {
        InMemoryReminderRepository repository = new InMemoryReminderRepository();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC);
        ReminderService reminderService = new ReminderService(repository, fixedClock);

        Reminder reminder = reminderService.createReminder("Call mom", "in 10 minutes");

        assertEquals("Call mom", reminder.getText());
        assertNotNull(reminder.getDueAt());
        assertEquals(Instant.parse("2026-04-18T10:10:00Z"), reminder.getDueAt());
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
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC);
        ReminderService reminderService = new ReminderService(repository, fixedClock);
        Instant base = Instant.parse("2026-04-18T10:01:00Z");
        Reminder reminder = new Reminder("xyz789", "meeting", base, true, fixedClock.instant(), fixedClock.instant());
        repository.setReminders(List.of(reminder));

        Optional<Reminder> snoozed = reminderService.snoozeReminder("xyz789", Duration.ofMinutes(15));

        assertTrue(snoozed.isPresent());
        assertFalse(snoozed.get().isCompleted());
        assertEquals(Instant.parse("2026-04-18T10:16:00Z"), snoozed.get().getDueAt());
    }

    @Test
    void snoozeReminder_usesCurrentTimeWhenReminderIsPastDue() {
        InMemoryReminderRepository repository = new InMemoryReminderRepository();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC);
        ReminderService reminderService = new ReminderService(repository, fixedClock);

        Reminder reminder = new Reminder("late-1", "late task", Instant.parse("2026-04-18T09:00:00Z"), false,
                fixedClock.instant(), fixedClock.instant());
        repository.setReminders(List.of(reminder));

        Optional<Reminder> snoozed = reminderService.snoozeReminder("late-1", Duration.ofMinutes(15));

        assertTrue(snoozed.isPresent());
        assertEquals(Instant.parse("2026-04-18T10:15:00Z"), snoozed.get().getDueAt());
    }

    @Test
    void deleteReminder_rejectsInvalidIds() {
        InMemoryReminderRepository repository = new InMemoryReminderRepository();
        ReminderService reminderService = new ReminderService(repository);
        repository.setReminders(List.of(new Reminder("id-1", "task", Instant.now(), false, Instant.now(), Instant.now())));

        assertTrue(reminderService.deleteReminder("id-1"));
        assertFalse(reminderService.deleteReminder(""));
    }
}
