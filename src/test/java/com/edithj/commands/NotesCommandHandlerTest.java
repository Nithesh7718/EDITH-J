package com.edithj.commands;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edithj.notes.Note;
import com.edithj.notes.NoteRepository;
import com.edithj.notes.NoteService;

class NotesCommandHandlerTest {

    @Test
    void handle_listCommandFormatsNotes() {
        NoteRepository noteRepository = mock(NoteRepository.class);
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));
        Note note = new Note("id-1", "Groceries", "Buy milk", Instant.now(), Instant.now());
        when(noteRepository.findAll()).thenReturn(List.of(note));

        String response = handler.handle(new CommandHandler.CommandContext("list notes", "list", "typed"));

        assertTrue(response.contains("Notes:"));
        assertTrue(response.contains("id-1"));
    }

    @Test
    void handle_updateCommandReturnsUpdatedMessage() {
        NoteRepository noteRepository = mock(NoteRepository.class);
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));
        Note existing = new Note("id-2", "Old", "Old", Instant.now(), Instant.now());
        when(noteRepository.findById("id-2")).thenReturn(Optional.of(existing));
        when(noteRepository.save(existing)).thenReturn(existing);

        String response = handler.handle(new CommandHandler.CommandContext("update", "update note id-2 | Updated content", "typed"));

        assertTrue(response.startsWith("Updated note id-2"));
    }

    @Test
    void handle_deleteCommandHandlesMissingId() {
        NoteRepository noteRepository = mock(NoteRepository.class);
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));
        when(noteRepository.deleteById("missing")).thenReturn(false);

        String response = handler.handle(new CommandHandler.CommandContext("delete", "delete note missing", "typed"));

        assertTrue(response.contains("could not find note missing".toLowerCase()));
    }

    @Test
    void handle_defaultCreatesNote() {
        NoteRepository noteRepository = mock(NoteRepository.class);
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));
        when(noteRepository.save(org.mockito.ArgumentMatchers.any(Note.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String response = handler.handle(new CommandHandler.CommandContext("note", "finish report", "typed"));

        assertTrue(response.startsWith("Saved note"));
    }
}
