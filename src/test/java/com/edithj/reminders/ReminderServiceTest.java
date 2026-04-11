package com.edithj.reminders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(reminderRepository);
    }

    @Test
    void createReminder_savesParsedReminder() {
        when(reminderRepository.save(org.mockito.ArgumentMatchers.any(Reminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reminder reminder = reminderService.createReminder("Call mom", "in 10 minutes");

        assertEquals("Call mom", reminder.getText());
        assertNotNull(reminder.getDueAt());
        verify(reminderRepository).save(org.mockito.ArgumentMatchers.any(Reminder.class));
    }

    @Test
    void listReminders_filtersCompletedAndSortsByDueDate() {
        Reminder done = new Reminder("1", "done", Instant.now().plusSeconds(60), true, Instant.now(), Instant.now());
        Reminder early = new Reminder("2", "early", Instant.now().plusSeconds(120), false, Instant.now(), Instant.now());
        Reminder late = new Reminder("3", "late", Instant.now().plusSeconds(240), false, Instant.now(), Instant.now());
        when(reminderRepository.findAll()).thenReturn(List.of(late, done, early));

        List<Reminder> reminders = reminderService.listReminders();

        assertEquals(List.of("2", "3"), reminders.stream().map(Reminder::getId).toList());
    }

    @Test
    void markDone_updatesExistingReminder() {
        Reminder reminder = new Reminder("abc123", "task", Instant.now().plusSeconds(60), false, Instant.now(), Instant.now());
        when(reminderRepository.findById("abc123")).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(reminder)).thenReturn(reminder);

        Optional<Reminder> updated = reminderService.markDone("abc123");

        assertTrue(updated.isPresent());
        assertTrue(updated.get().isCompleted());
        verify(reminderRepository).save(reminder);
    }

    @Test
    void snoozeReminder_movesDueDateForward() {
        Instant base = Instant.now().plusSeconds(60);
        Reminder reminder = new Reminder("xyz789", "meeting", base, true, Instant.now(), Instant.now());
        when(reminderRepository.findById("xyz789")).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(reminder)).thenReturn(reminder);

        Optional<Reminder> snoozed = reminderService.snoozeReminder("xyz789", Duration.ofMinutes(15));

        assertTrue(snoozed.isPresent());
        assertFalse(snoozed.get().isCompleted());
        assertTrue(snoozed.get().getDueAt().isAfter(base));
    }

    @Test
    void deleteReminder_rejectsInvalidIds() {
        when(reminderRepository.deleteById("id-1")).thenReturn(true);

        assertTrue(reminderService.deleteReminder("id-1"));
        assertFalse(reminderService.deleteReminder(""));
    }
}
