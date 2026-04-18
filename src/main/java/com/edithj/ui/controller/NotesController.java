package com.edithj.ui.controller;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.edithj.notes.Note;
import com.edithj.notes.NoteService;
import com.edithj.storage.RepositoryFactory;
import com.edithj.ui.model.NoteViewModel;

public class NotesController {

    private static final DateTimeFormatter UPDATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NoteService noteService;

    public NotesController() {
        this(new NoteService(RepositoryFactory.createNoteRepository()));
    }

    public NotesController(NoteService noteService) {
        this.noteService = noteService;
    }

    public NoteViewModel createNote(String content) {
        Note note = noteService.createNote(content);
        return toViewModel(note);
    }

    public List<NoteViewModel> listNotes() {
        return noteService.listNotes().stream()
                .map(this::toViewModel)
                .toList();
    }

    public List<NoteViewModel> searchNotes(String query) {
        return noteService.searchNotes(query).stream()
                .map(this::toViewModel)
                .toList();
    }

    public Optional<NoteViewModel> updateNote(String noteId, String content) {
        return noteService.updateNote(noteId, content)
                .map(this::toViewModel);
    }

    public boolean deleteNote(String noteId) {
        return noteService.deleteNote(noteId);
    }

    private NoteViewModel toViewModel(Note note) {
        String updatedAt = note.getUpdatedAt() == null
                ? ""
                : UPDATED_AT_FORMAT.format(note.getUpdatedAt().atZone(ZoneId.systemDefault()));
        return new NoteViewModel(note.getId(), note.getTitle(), note.getContent(), updatedAt);
    }
}
