package com.edithj.reminders;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.edithj.storage.JsonStorage;
import com.edithj.storage.StoragePaths;

public class FileReminderRepository implements ReminderRepository {

    private static final TypeReference<List<Reminder>> REMINDER_LIST_TYPE = new TypeReference<>() {
    };

    private final JsonStorage storage;

    public FileReminderRepository() {
        this(StoragePaths.remindersPath());
    }

    public FileReminderRepository(Path storagePath) {
        this.storage = new JsonStorage(storagePath);
    }

    @Override
    public List<Reminder> findAll() {
        return storage.readList(REMINDER_LIST_TYPE);
    }

    @Override
    public Optional<Reminder> findById(String reminderId) {
        if (reminderId == null || reminderId.isBlank()) {
            return Optional.empty();
        }

        return findAll().stream()
                .filter(reminder -> reminderId.equals(reminder.getId()))
                .findFirst();
    }

    @Override
    public Reminder save(Reminder reminder) {
        if (reminder == null) {
            throw new IllegalArgumentException("Reminder cannot be null");
        }

        List<Reminder> reminders = findAll();
        int index = indexOf(reminders, reminder.getId());

        if (reminder.getCreatedAt() == null) {
            reminder.setCreatedAt(Instant.now());
        }
        if (reminder.getUpdatedAt() == null) {
            reminder.setUpdatedAt(Instant.now());
        }

        if (index >= 0) {
            reminders.set(index, reminder);
        } else {
            reminders.add(reminder);
        }

        storage.writeList(reminders);
        return reminder;
    }

    @Override
    public boolean deleteById(String reminderId) {
        if (reminderId == null || reminderId.isBlank()) {
            return false;
        }

        List<Reminder> reminders = findAll();
        int index = indexOf(reminders, reminderId);
        if (index < 0) {
            return false;
        }

        reminders.remove(index);
        storage.writeList(reminders);
        return true;
    }

    private int indexOf(List<Reminder> reminders, String reminderId) {
        for (int i = 0; i < reminders.size(); i++) {
            if (reminderId.equals(reminders.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
}
