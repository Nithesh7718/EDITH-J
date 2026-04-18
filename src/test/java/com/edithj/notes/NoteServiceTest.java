package com.edithj.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class NoteServiceTest {

    @Test
    void createNote_derivesTitleAndSaves() {
        InMemoryNoteRepository repository = new InMemoryNoteRepository();
        NoteService noteService = new NoteService(repository);

        Note created = noteService.createNote("Buy groceries and vegetables");

        assertEquals("Buy groceries and vegetables", created.getTitle());
        assertEquals("Buy groceries and vegetables", created.getContent());
        assertEquals(1, repository.savedNotes().size());
    }

    @Test
    void listNotes_sortsByMostRecentUpdatedAtFirst() {
        InMemoryNoteRepository repository = new InMemoryNoteRepository();
        NoteService noteService = new NoteService(repository);
        Note older = new Note("1", "old", "old", Instant.now().minusSeconds(60), Instant.now().minusSeconds(60));
        Note newer = new Note("2", "new", "new", Instant.now(), Instant.now());
        repository.setNotes(List.of(older, newer));

        List<Note> notes = noteService.listNotes();

        assertEquals(List.of("2", "1"), notes.stream().map(Note::getId).toList());
    }

    @Test
    void updateNote_returnsEmptyForInvalidId() {
        NoteService noteService = new NoteService(new InMemoryNoteRepository());
        Optional<Note> updated = noteService.updateNote("", "new text");

        assertTrue(updated.isEmpty());
    }

    @Test
    void updateNote_updatesExistingNote() {
        InMemoryNoteRepository repository = new InMemoryNoteRepository();
        NoteService noteService = new NoteService(repository);
        Note existing = new Note("abc123", "old", "old", Instant.now(), Instant.now());
        repository.setNotes(List.of(existing));

        Optional<Note> updated = noteService.updateNote("abc123", "Updated body");

        assertTrue(updated.isPresent());
        assertEquals("Updated body", updated.get().getContent());
        assertEquals("Updated body", updated.get().getTitle());
    }

    @Test
    void deleteNote_returnsRepositoryResult() {
        InMemoryNoteRepository repository = new InMemoryNoteRepository();
        NoteService noteService = new NoteService(repository);
        repository.setNotes(List.of(new Note("n-1", "title", "content", Instant.now(), Instant.now())));

        assertTrue(noteService.deleteNote("n-1"));
        assertFalse(noteService.deleteNote(""));
    }

    private static final class InMemoryNoteRepository implements NoteRepository {

        private final List<Note> notes = new ArrayList<>();
        private final List<Note> savedNotes = new ArrayList<>();

        void setNotes(List<Note> values) {
            notes.clear();
            notes.addAll(values);
        }

        List<Note> savedNotes() {
            return savedNotes;
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
}
