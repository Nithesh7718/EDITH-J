package com.edithj.notes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.edithj.util.ValidationUtils;

public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public Note createNote(String rawContent) {
        String content = ValidationUtils.validateNonEmpty(rawContent, "Note content");
        Note note = Note.newNote(deriveTitle(content), content);
        return noteRepository.save(note);
    }

    public List<Note> listNotes() {
        return noteRepository.findAll().stream()
                .sorted(Comparator.comparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<Note> searchNotes(String rawQuery) {
        String query = ValidationUtils.normalize(rawQuery);
        if (query.isBlank()) {
            return listNotes();
        }

        return listNotes().stream()
                .filter(note -> note.matches(query))
                .toList();
    }

    public Optional<Note> updateNote(String noteId, String rawContent) {
        if (!ValidationUtils.isValidId(noteId)) {
            return Optional.empty();
        }

        String content = ValidationUtils.normalize(rawContent);
        if (content.isBlank()) {
            return Optional.empty();
        }

        Optional<Note> existing = noteRepository.findById(noteId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Note note = existing.get();
        note.update(deriveTitle(content), content);
        return Optional.of(noteRepository.save(note));
    }

    public boolean deleteNote(String noteId) {
        if (!ValidationUtils.isValidId(noteId)) {
            return false;
        }
        return noteRepository.deleteById(noteId);
    }

    private String deriveTitle(String content) {
        int firstLineBreak = content.indexOf('\n');
        String firstLine = firstLineBreak < 0 ? content : content.substring(0, firstLineBreak);
        return ValidationUtils.truncate(firstLine.trim(), 40);
    }
}
