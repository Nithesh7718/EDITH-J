package com.edithj.commands;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.notes.InMemoryNoteRepository;
import com.edithj.notes.Note;
import com.edithj.notes.NoteService;

class NotesCommandHandlerTest {

    @Test
    void handle_listCommandFormatsNotes() {
        InMemoryNoteRepository noteRepository = new InMemoryNoteRepository();
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));
        Note note = new Note("id-1", "Groceries", "Buy milk", Instant.now(), Instant.now());
        noteRepository.setNotes(List.of(note));

        String response = handler.handle(new CommandHandler.CommandContext("list notes", "list", "typed"));

        assertTrue(response.contains("Notes:"));
        assertTrue(response.contains("id-1"));
    }

    @Test
    void handle_updateCommandReturnsUpdatedMessage() {
        InMemoryNoteRepository noteRepository = new InMemoryNoteRepository();
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));
        Note existing = new Note("id-2", "Old", "Old", Instant.now(), Instant.now());
        noteRepository.setNotes(List.of(existing));

        String response = handler.handle(new CommandHandler.CommandContext("update", "update note id-2 | Updated content", "typed"));

        assertTrue(response.startsWith("Updated note id-2"));
    }

    @Test
    void handle_deleteCommandHandlesMissingId() {
        InMemoryNoteRepository noteRepository = new InMemoryNoteRepository();
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));
        noteRepository.setNotes(List.of());

        String response = handler.handle(new CommandHandler.CommandContext("delete", "delete note missing", "typed"));

        assertTrue(response.contains("could not find note missing".toLowerCase()));
    }

    @Test
    void handle_defaultCreatesNote() {
        InMemoryNoteRepository noteRepository = new InMemoryNoteRepository();
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));

        String response = handler.handle(new CommandHandler.CommandContext("note", "finish report", "typed"));

        assertTrue(response.startsWith("Saved note"));
    }

    @Test
    void handle_updateWithoutDelimiterReturnsGuidance() {
        InMemoryNoteRepository noteRepository = new InMemoryNoteRepository();
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));

        String response = handler.handle(new CommandHandler.CommandContext("update", "update note id-2 updated content", "typed"));

        assertTrue(response.contains("Use update note like"));
    }

    @Test
    void handle_searchWithoutQueryReturnsGuidance() {
        InMemoryNoteRepository noteRepository = new InMemoryNoteRepository();
        NotesCommandHandler handler = new NotesCommandHandler(new NoteService(noteRepository));

        String response = handler.handle(new CommandHandler.CommandContext("search", "search notes", "typed"));

        assertTrue(response.contains("Please provide a search query"));
    }
}
