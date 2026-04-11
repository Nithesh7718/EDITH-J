package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.edithj.notes.Note;
import com.edithj.notes.NoteService;

@ExtendWith(MockitoExtension.class)
class NotesCommandHandlerTest {

    @Mock
    private NoteService noteService;

    private NotesCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NotesCommandHandler(noteService);
    }

    @Test
    void handle_listCommandFormatsNotes() {
        Note note = new Note("id-1", "Groceries", "Buy milk", Instant.now(), Instant.now());
        when(noteService.listNotes()).thenReturn(List.of(note));

        String response = handler.handle(new CommandHandler.CommandContext("list notes", "list", "typed"));

        assertTrue(response.contains("Notes:"));
        assertTrue(response.contains("id-1"));
    }

    @Test
    void handle_updateCommandReturnsUpdatedMessage() {
        Note updated = new Note("id-2", "Updated", "Updated", Instant.now(), Instant.now());
        when(noteService.updateNote("id-2", "Updated content")).thenReturn(Optional.of(updated));

        String response = handler.handle(new CommandHandler.CommandContext("update", "update note id-2 | Updated content", "typed"));

        assertTrue(response.startsWith("Updated note id-2"));
    }

    @Test
    void handle_deleteCommandHandlesMissingId() {
        when(noteService.deleteNote("missing")).thenReturn(false);

        String response = handler.handle(new CommandHandler.CommandContext("delete", "delete note missing", "typed"));

        assertTrue(response.contains("could not find note missing".toLowerCase()));
    }

    @Test
    void handle_defaultCreatesNote() {
        Note created = new Note("new-1", "Task", "Task", Instant.now(), Instant.now());
        when(noteService.createNote("finish report")).thenReturn(created);

        String response = handler.handle(new CommandHandler.CommandContext("note", "finish report", "typed"));

        assertTrue(response.startsWith("Saved note new-1"));
    }
}
