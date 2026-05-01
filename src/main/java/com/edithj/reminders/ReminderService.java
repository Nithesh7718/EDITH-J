package com.edithj.reminders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.edithj.util.TimeParser;
import com.edithj.util.ValidationUtils;

public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final Clock clock;

    public ReminderService(ReminderRepository reminderRepository) {
        this(reminderRepository, Clock.systemDefaultZone());
    }

    public ReminderService(ReminderRepository reminderRepository, Clock clock) {
        this.reminderRepository = reminderRepository;
        this.clock = clock;
    }

    public Reminder createReminder(String rawText, String rawTimeHint) {
        String text = requireNonBlank(rawText, "Reminder text");

        Instant dueAt = TimeParser.parseTimeHint(rawTimeHint, clock);
        if (dueAt == null) {
            throw new IllegalArgumentException("Unable to parse time hint: " + rawTimeHint);
        }

        Reminder reminder = Reminder.newReminder(toTitleCase(text), dueAt);
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
        if (!hasUsableId(reminderId)) {
            return Optional.empty();
        }

        String normalizedId = reminderId.trim();
        Optional<Reminder> existing = reminderRepository.findById(normalizedId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Reminder reminder = existing.get();
        reminder.markDone();
        return Optional.of(reminderRepository.save(reminder));
    }

    public boolean deleteReminder(String reminderId) {
        if (!hasUsableId(reminderId)) {
            return false;
        }
        return reminderRepository.deleteById(reminderId.trim());
    }

    public Optional<Reminder> snoozeReminder(String reminderId, Duration duration) {
        if (!hasUsableId(reminderId) || duration == null || duration.isNegative() || duration.isZero()) {
            return Optional.empty();
        }

        String normalizedId = reminderId.trim();
        Optional<Reminder> existing = reminderRepository.findById(normalizedId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Reminder reminder = existing.get();
        Instant now = clock.instant();
        Instant base = reminder.getDueAt() == null || reminder.getDueAt().isBefore(now)
                ? now
                : reminder.getDueAt();

        reminder.setDueAt(base.plus(duration));
        reminder.setCompleted(false);
        reminder.setUpdatedAt(now);

        return Optional.of(reminderRepository.save(reminder));
    }

    private String requireNonBlank(String rawText, String fieldName) {
        String cleaned = cleanText(rawText);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return cleaned;
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasUsableId(String id) {
        return id != null && !id.trim().isBlank();
    }

    private String toTitleCase(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
