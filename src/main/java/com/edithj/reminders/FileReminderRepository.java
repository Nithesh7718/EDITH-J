package com.edithj.reminders;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class FileReminderRepository implements ReminderRepository {

    private static final TypeReference<List<Reminder>> REMINDER_LIST_TYPE = new TypeReference<>() {
    };

    private final Path storagePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FileReminderRepository() {
        this(Paths.get("src", "main", "resources", "data", "reminders.json"));
    }

    public FileReminderRepository(Path storagePath) {
        this.storagePath = storagePath;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ensureStorageExists();
    }

    @Override
    public List<Reminder> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(readReminders());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Reminder> findById(String reminderId) {
        if (reminderId == null || reminderId.isBlank()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return readReminders().stream()
                    .filter(reminder -> reminderId.equals(reminder.getId()))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Reminder save(Reminder reminder) {
        if (reminder == null) {
            throw new IllegalArgumentException("Reminder cannot be null");
        }

        lock.writeLock().lock();
        try {
            List<Reminder> reminders = readReminders();
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

            writeReminders(reminders);
            return reminder;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteById(String reminderId) {
        if (reminderId == null || reminderId.isBlank()) {
            return false;
        }

        lock.writeLock().lock();
        try {
            List<Reminder> reminders = readReminders();
            int index = indexOf(reminders, reminderId);
            if (index < 0) {
                return false;
            }

            reminders.remove(index);
            writeReminders(reminders);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private int indexOf(List<Reminder> reminders, String reminderId) {
        for (int i = 0; i < reminders.size(); i++) {
            if (reminderId.equals(reminders.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private List<Reminder> readReminders() {
        if (!Files.exists(storagePath)) {
            return new ArrayList<>();
        }

        try (InputStream input = Files.newInputStream(storagePath)) {
            return objectMapper.readValue(input, REMINDER_LIST_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read reminders from " + storagePath, exception);
        }
    }

    private void writeReminders(List<Reminder> reminders) {
        try (OutputStream output = Files.newOutputStream(storagePath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(output, reminders);
        } catch (IOException ioException) {
            throw new IllegalStateException("Unable to write reminders to " + storagePath, ioException);
        }
    }

    private void ensureStorageExists() {
        try {
            Files.createDirectories(storagePath.getParent());
            if (!Files.exists(storagePath)) {
                writeReminders(new ArrayList<>());
            }
        } catch (IOException ioException) {
            throw new IllegalStateException("Unable to initialize storage at " + storagePath, ioException);
        }
    }
}
