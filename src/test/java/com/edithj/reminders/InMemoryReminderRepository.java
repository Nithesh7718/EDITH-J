package com.edithj.reminders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryReminderRepository implements ReminderRepository {

    private final List<Reminder> reminders = new ArrayList<>();
    private final List<Reminder> savedReminders = new ArrayList<>();

    public void setReminders(List<Reminder> values) {
        reminders.clear();
        reminders.addAll(values);
    }

    public List<Reminder> savedReminders() {
        return new ArrayList<>(savedReminders);
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
