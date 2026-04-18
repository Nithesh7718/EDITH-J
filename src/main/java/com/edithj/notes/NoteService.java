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
        String content = requireNonBlank(rawContent, "Note content");
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
        if (!hasUsableId(noteId)) {
            return Optional.empty();
        }

        String content = cleanText(rawContent);
        if (content.isBlank()) {
            return Optional.empty();
        }

        String normalizedId = noteId.trim();
        Optional<Note> existing = noteRepository.findById(normalizedId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Note note = existing.get();
        note.update(deriveTitle(content), content);
        return Optional.of(noteRepository.save(note));
    }

    public boolean deleteNote(String noteId) {
        if (!hasUsableId(noteId)) {
            return false;
        }
        return noteRepository.deleteById(noteId.trim());
    }

    private String deriveTitle(String content) {
        int firstLineBreak = content.indexOf('\n');
        String firstLine = firstLineBreak < 0 ? content : content.substring(0, firstLineBreak);
        String truncated = ValidationUtils.truncate(firstLine.trim(), 40);
        return toTitleCase(truncated);
    }

    private String requireNonBlank(String rawText, String fieldName) {
        String cleaned = cleanText(rawText);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return cleaned;
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasUsableId(String id) {
        return id != null && !id.trim().isBlank();
    }

    private String toTitleCase(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
