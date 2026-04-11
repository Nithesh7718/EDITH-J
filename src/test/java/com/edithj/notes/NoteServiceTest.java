package com.edithj.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository);
    }

    @Test
    void createNote_derivesTitleAndSaves() {
        when(noteRepository.save(org.mockito.ArgumentMatchers.any(Note.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Note created = noteService.createNote("Buy groceries and vegetables");

        assertEquals("Buy groceries and vegetables", created.getTitle());
        assertEquals("Buy groceries and vegetables", created.getContent());
        verify(noteRepository).save(org.mockito.ArgumentMatchers.any(Note.class));
    }

    @Test
    void listNotes_sortsByMostRecentUpdatedAtFirst() {
        Note older = new Note("1", "old", "old", Instant.now().minusSeconds(60), Instant.now().minusSeconds(60));
        Note newer = new Note("2", "new", "new", Instant.now(), Instant.now());
        when(noteRepository.findAll()).thenReturn(List.of(older, newer));

        List<Note> notes = noteService.listNotes();

        assertEquals(List.of("2", "1"), notes.stream().map(Note::getId).toList());
    }

    @Test
    void updateNote_returnsEmptyForInvalidId() {
        Optional<Note> updated = noteService.updateNote("", "new text");

        assertTrue(updated.isEmpty());
    }

    @Test
    void updateNote_updatesExistingNote() {
        Note existing = new Note("abc123", "old", "old", Instant.now(), Instant.now());
        when(noteRepository.findById("abc123")).thenReturn(Optional.of(existing));
        when(noteRepository.save(existing)).thenReturn(existing);

        Optional<Note> updated = noteService.updateNote("abc123", "Updated body");

        assertTrue(updated.isPresent());
        assertEquals("Updated body", updated.get().getContent());
        assertEquals("Updated body", updated.get().getTitle());
        verify(noteRepository).save(existing);
    }

    @Test
    void deleteNote_returnsRepositoryResult() {
        when(noteRepository.deleteById("n-1")).thenReturn(true);

        assertTrue(noteService.deleteNote("n-1"));
        assertFalse(noteService.deleteNote(""));
    }
}
