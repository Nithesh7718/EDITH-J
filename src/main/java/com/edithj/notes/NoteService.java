package com.edithj.notes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public Note createNote(String rawContent) {
        String content = normalize(rawContent);
        if (content.isBlank()) {
            throw new IllegalArgumentException("Note content cannot be empty.");
        }

        Note note = Note.newNote(deriveTitle(content), content);
        return noteRepository.save(note);
    }

    public List<Note> listNotes() {
        return noteRepository.findAll().stream()
                .sorted(Comparator.comparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<Note> searchNotes(String rawQuery) {
        String query = normalize(rawQuery);
        if (query.isBlank()) {
            return listNotes();
        }

        return listNotes().stream()
                .filter(note -> note.matches(query))
                .toList();
    }

    public Optional<Note> updateNote(String noteId, String rawContent) {
        String id = normalize(noteId);
        String content = normalize(rawContent);
        if (id.isBlank() || content.isBlank()) {
            return Optional.empty();
        }

        Optional<Note> existing = noteRepository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Note note = existing.get();
        note.update(deriveTitle(content), content);
        return Optional.of(noteRepository.save(note));
    }

    public boolean deleteNote(String noteId) {
        String id = normalize(noteId);
        if (id.isBlank()) {
            return false;
        }
        return noteRepository.deleteById(id);
    }

    private String deriveTitle(String content) {
        int firstLineBreak = content.indexOf('\n');
        String firstLine = firstLineBreak < 0 ? content : content.substring(0, firstLineBreak);
        String compact = firstLine.trim();
        if (compact.length() <= 40) {
            return compact;
        }
        return compact.substring(0, 40) + "...";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
