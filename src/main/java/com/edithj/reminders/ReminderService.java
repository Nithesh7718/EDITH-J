package com.edithj.reminders;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.edithj.util.TimeParser;
import com.edithj.util.ValidationUtils;

public class ReminderService {

    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    public Reminder createReminder(String rawText, String rawTimeHint) {
        String text = ValidationUtils.validateNonEmpty(rawText, "Reminder text");

        Instant dueAt = TimeParser.parseTimeHint(rawTimeHint);
        if (dueAt == null) {
            throw new IllegalArgumentException("Unable to parse time hint: " + rawTimeHint);
        }

        Reminder reminder = Reminder.newReminder(text, dueAt);
        return reminderRepository.save(reminder);
    }

    public List<Reminder> listReminders() {
        return reminderRepository.findAll().stream()
                .filter(r -> !r.isCompleted())
                .sorted(Comparator.comparing(Reminder::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<Reminder> listAllReminders() {
        return reminderRepository.findAll().stream()
                .sorted(Comparator.comparing(Reminder::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<Reminder> searchReminders(String rawQuery) {
        String query = ValidationUtils.normalize(rawQuery);
        if (query.isBlank()) {
            return listReminders();
        }

        return listReminders().stream()
                .filter(reminder -> reminder.matches(query))
                .toList();
    }

    public Optional<Reminder> markDone(String reminderId) {
        if (!ValidationUtils.isValidId(reminderId)) {
            return Optional.empty();
        }

        Optional<Reminder> existing = reminderRepository.findById(reminderId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Reminder reminder = existing.get();
        reminder.markDone();
        return Optional.of(reminderRepository.save(reminder));
    }

    public boolean deleteReminder(String reminderId) {
        if (!ValidationUtils.isValidId(reminderId)) {
            return false;
        }
        return reminderRepository.deleteById(reminderId);
    }
}
