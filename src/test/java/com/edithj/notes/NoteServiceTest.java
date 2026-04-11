package com.edithj.notes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteServiceTest {

    @Test
    void createNote_derivesTitleAndSaves() {
    NoteRepository repository = org.mockito.Mockito.mock(NoteRepository.class);
    NoteService noteService = new NoteService(repository);
    when(repository.save(org.mockito.ArgumentMatchers.any(Note.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Note created = noteService.createNote("Buy groceries and vegetables");

        assertEquals("Buy groceries and vegetables", created.getTitle());
        assertEquals("Buy groceries and vegetables", created.getContent());
        verify(repository).save(org.mockito.ArgumentMatchers.any(Note.class));
    }

    @Test
    void listNotes_sortsByMostRecentUpdatedAtFirst() {
        NoteRepository repository = org.mockito.Mockito.mock(NoteRepository.class);
        NoteService noteService = new NoteService(repository);
        Note older = new Note("1", "old", "old", Instant.now().minusSeconds(60), Instant.now().minusSeconds(60));
        Note newer = new Note("2", "new", "new", Instant.now(), Instant.now());
        when(repository.findAll()).thenReturn(List.of(older, newer));

        List<Note> notes = noteService.listNotes();

        assertEquals(List.of("2", "1"), notes.stream().map(Note::getId).toList());
    }

    @Test
    void updateNote_returnsEmptyForInvalidId() {
        NoteService noteService = new NoteService(org.mockito.Mockito.mock(NoteRepository.class));
        Optional<Note> updated = noteService.updateNote("", "new text");

        assertTrue(updated.isEmpty());
    }

    @Test
    void updateNote_updatesExistingNote() {
        NoteRepository repository = org.mockito.Mockito.mock(NoteRepository.class);
        NoteService noteService = new NoteService(repository);
        Note existing = new Note("abc123", "old", "old", Instant.now(), Instant.now());
        when(repository.findById("abc123")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Optional<Note> updated = noteService.updateNote("abc123", "Updated body");

        assertTrue(updated.isPresent());
        assertEquals("Updated body", updated.get().getContent());
        assertEquals("Updated body", updated.get().getTitle());
        verify(repository).save(existing);
    }

    @Test
    void deleteNote_returnsRepositoryResult() {
        NoteRepository repository = org.mockito.Mockito.mock(NoteRepository.class);
        NoteService noteService = new NoteService(repository);
        when(repository.deleteById("n-1")).thenReturn(true);

        assertTrue(noteService.deleteNote("n-1"));
        assertFalse(noteService.deleteNote(""));
    }
}
