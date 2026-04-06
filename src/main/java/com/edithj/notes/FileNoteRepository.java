package com.edithj.notes;

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

public class FileNoteRepository implements NoteRepository {

    private static final TypeReference<List<Note>> NOTE_LIST_TYPE = new TypeReference<>() {
    };

    private final Path storagePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FileNoteRepository() {
        this(Paths.get("src", "main", "resources", "data", "notes.json"));
    }

    public FileNoteRepository(Path storagePath) {
        this.storagePath = storagePath;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ensureStorageExists();
    }

    @Override
    public List<Note> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(readNotes());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Note> findById(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return readNotes().stream()
                    .filter(note -> noteId.equals(note.getId()))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Note save(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }

        lock.writeLock().lock();
        try {
            List<Note> notes = readNotes();
            int index = indexOf(notes, note.getId());

            if (note.getCreatedAt() == null) {
                note.setCreatedAt(Instant.now());
            }
            if (note.getUpdatedAt() == null) {
                note.setUpdatedAt(Instant.now());
            }

            if (index >= 0) {
                notes.set(index, note);
            } else {
                notes.add(note);
            }

            writeNotes(notes);
            return note;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteById(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return false;
        }

        lock.writeLock().lock();
        try {
            List<Note> notes = readNotes();
            boolean removed = notes.removeIf(note -> noteId.equals(note.getId()));
            if (removed) {
                writeNotes(notes);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private int indexOf(List<Note> notes, String noteId) {
        for (int i = 0; i < notes.size(); i++) {
            if (noteId != null && noteId.equals(notes.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private List<Note> readNotes() {
        ensureStorageExists();
        try (InputStream inputStream = Files.newInputStream(storagePath)) {
            List<Note> notes = objectMapper.readValue(inputStream, NOTE_LIST_TYPE);
            return notes == null ? new ArrayList<>() : notes;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read notes storage", exception);
        }
    }

    private void writeNotes(List<Note> notes) {
        ensureStorageExists();
        try (OutputStream outputStream = Files.newOutputStream(storagePath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, notes);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write notes storage", exception);
        }
    }

    private void ensureStorageExists() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(storagePath)) {
                Files.writeString(storagePath, "[]");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize notes storage", exception);
        }
    }
}
