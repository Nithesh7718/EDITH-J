package com.edithj.reminders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReminderServiceTest {

    @Test
    void createReminder_savesParsedReminder() {
    ReminderRepository repository = org.mockito.Mockito.mock(ReminderRepository.class);
    ReminderService reminderService = new ReminderService(repository);
    when(repository.save(org.mockito.ArgumentMatchers.any(Reminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reminder reminder = reminderService.createReminder("Call mom", "in 10 minutes");

        assertEquals("Call mom", reminder.getText());
        assertNotNull(reminder.getDueAt());
        verify(repository).save(org.mockito.ArgumentMatchers.any(Reminder.class));
    }

    @Test
    void listReminders_filtersCompletedAndSortsByDueDate() {
        ReminderRepository repository = org.mockito.Mockito.mock(ReminderRepository.class);
        ReminderService reminderService = new ReminderService(repository);
        Reminder done = new Reminder("1", "done", Instant.now().plusSeconds(60), true, Instant.now(), Instant.now());
        Reminder early = new Reminder("2", "early", Instant.now().plusSeconds(120), false, Instant.now(), Instant.now());
        Reminder late = new Reminder("3", "late", Instant.now().plusSeconds(240), false, Instant.now(), Instant.now());
        when(repository.findAll()).thenReturn(List.of(late, done, early));

        List<Reminder> reminders = reminderService.listReminders();

        assertEquals(List.of("2", "3"), reminders.stream().map(Reminder::getId).toList());
    }

    @Test
    void markDone_updatesExistingReminder() {
        ReminderRepository repository = org.mockito.Mockito.mock(ReminderRepository.class);
        ReminderService reminderService = new ReminderService(repository);
        Reminder reminder = new Reminder("abc123", "task", Instant.now().plusSeconds(60), false, Instant.now(), Instant.now());
        when(repository.findById("abc123")).thenReturn(Optional.of(reminder));
        when(repository.save(reminder)).thenReturn(reminder);

        Optional<Reminder> updated = reminderService.markDone("abc123");

        assertTrue(updated.isPresent());
        assertTrue(updated.get().isCompleted());
        verify(repository).save(reminder);
    }

    @Test
    void snoozeReminder_movesDueDateForward() {
        ReminderRepository repository = org.mockito.Mockito.mock(ReminderRepository.class);
        ReminderService reminderService = new ReminderService(repository);
        Instant base = Instant.now().plusSeconds(60);
        Reminder reminder = new Reminder("xyz789", "meeting", base, true, Instant.now(), Instant.now());
        when(repository.findById("xyz789")).thenReturn(Optional.of(reminder));
        when(repository.save(reminder)).thenReturn(reminder);

        Optional<Reminder> snoozed = reminderService.snoozeReminder("xyz789", Duration.ofMinutes(15));

        assertTrue(snoozed.isPresent());
        assertFalse(snoozed.get().isCompleted());
        assertTrue(snoozed.get().getDueAt().isAfter(base));
    }

    @Test
    void deleteReminder_rejectsInvalidIds() {
        ReminderRepository repository = org.mockito.Mockito.mock(ReminderRepository.class);
        ReminderService reminderService = new ReminderService(repository);
        when(repository.deleteById("id-1")).thenReturn(true);

        assertTrue(reminderService.deleteReminder("id-1"));
        assertFalse(reminderService.deleteReminder(""));
    }
}
