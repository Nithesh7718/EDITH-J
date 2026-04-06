package com.edithj.notes;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.edithj.storage.JsonStorage;
import com.edithj.storage.StoragePaths;

public class FileNoteRepository implements NoteRepository {

    private static final TypeReference<List<Note>> NOTE_LIST_TYPE = new TypeReference<>() {
    };

    private final JsonStorage storage;

    public FileNoteRepository() {
        this(StoragePaths.notesPath());
    }

    public FileNoteRepository(Path storagePath) {
        this.storage = new JsonStorage(storagePath);
    }

    @Override
    public List<Note> findAll() {
        return storage.readList(NOTE_LIST_TYPE);
    }

    @Override
    public Optional<Note> findById(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return Optional.empty();
        }

        return findAll().stream()
                .filter(note -> noteId.equals(note.getId()))
                .findFirst();
    }

    @Override
    public Note save(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }

        List<Note> notes = findAll();
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

        storage.writeList(notes);
        return note;
    }

    @Override
    public boolean deleteById(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return false;
        }

        List<Note> notes = findAll();
        boolean removed = notes.removeIf(note -> noteId.equals(note.getId()));
        if (removed) {
            storage.writeList(notes);
        }
        return removed;
    }

    private int indexOf(List<Note> notes, String noteId) {
        for (int i = 0; i < notes.size(); i++) {
            if (noteId != null && noteId.equals(notes.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
}
