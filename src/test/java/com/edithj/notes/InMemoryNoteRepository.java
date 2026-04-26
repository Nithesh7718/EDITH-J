package com.edithj.notes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryNoteRepository implements NoteRepository {

    private final List<Note> notes = new ArrayList<>();
    private final List<Note> savedNotes = new ArrayList<>();

    public void setNotes(List<Note> values) {
        notes.clear();
        notes.addAll(values);
    }

    public List<Note> savedNotes() {
        return new ArrayList<>(savedNotes);
    }

    @Override
    public List<Note> findAll() {
        return new ArrayList<>(notes);
    }

    @Override
    public Optional<Note> findById(String noteId) {
        return notes.stream().filter(note -> note.getId().equals(noteId)).findFirst();
    }

    @Override
    public Note save(Note note) {
        savedNotes.add(note);
        notes.removeIf(existing -> existing.getId().equals(note.getId()));
        notes.add(note);
        return note;
    }

    @Override
    public boolean deleteById(String noteId) {
        return notes.removeIf(existing -> existing.getId().equals(noteId));
    }
}
