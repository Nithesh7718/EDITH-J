package com.edithj.commands;

import java.util.List;
import java.util.Optional;

import com.edithj.assistant.IntentType;
import com.edithj.notes.Note;
import com.edithj.notes.NoteService;
import com.edithj.storage.RepositoryFactory;

public class NotesCommandHandler implements CommandHandler {

    private final NoteService noteService;

    public NotesCommandHandler() {
        this(new NoteService(RepositoryFactory.createNoteRepository()));
    }

    public NotesCommandHandler(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public IntentType intentType() {
        return IntentType.NOTES;
    }

    @Override
    public String handle(CommandContext context) {
        String payload = sanitizePayload(context);
        if (payload.isBlank()) {
            return "Try one of these: note buy milk, list notes, search notes milk, update note <id> | <text>, delete note <id>.";
        }

        try {
            if (startsWithAny(payload, "list", "show")) {
                return formatNotes(noteService.listNotes(), "No notes yet.");
            }

            if (startsWithAny(payload, "search", "find")) {
                String query = stripKeyword(payload, "search", "find").replaceFirst("(?i)^notes?\\s*", "").trim();
                if (query.isBlank()) {
                    return "Please provide a search query, for example: search notes project.";
                }
                return formatNotes(noteService.searchNotes(query), "No notes found for: " + query + ".");
            }

            if (startsWithAny(payload, "update", "edit")) {
                return handleUpdate(payload);
            }

            if (startsWithAny(payload, "delete", "remove")) {
                String id = stripKeyword(payload, "delete", "remove").replaceFirst("(?i)^notes?\\s*", "").trim();
                if (id.isBlank()) {
                    return "Please provide the note id to delete, for example: delete note <id>.";
                }
                boolean deleted = noteService.deleteNote(id);
                return deleted ? "Deleted note " + id + "." : "I could not find note " + id + ".";
            }

            Note created = noteService.createNote(payload);
            return "Saved note " + created.getId() + ": " + created.summary();
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        } catch (RuntimeException exception) {
            return genericError();
        }
    }

    private String handleUpdate(String payload) {
        String updatePart = stripKeyword(payload, "update", "edit").replaceFirst("(?i)^notes?\\s*", "").trim();
        String[] parts = updatePart.split("\\|", 2);
        if (parts.length < 2) {
            return "Use update note like: update note <id> | <new content>.";
        }

        String id = parts[0].trim();
        String content = parts[1].trim();
        if (id.isBlank() || content.isBlank()) {
            return "Use update note like: update note <id> | <new content>.";
        }

        Optional<Note> updated = noteService.updateNote(id, content);
        return updated.map(note -> "Updated note " + note.getId() + ": " + note.summary())
                .orElse("I could not find note " + id + ".");
    }

    private String formatNotes(List<Note> notes, String emptyMessage) {
        if (notes.isEmpty()) {
            return emptyMessage;
        }

        int max = Math.min(notes.size(), 8);
        StringBuilder response = new StringBuilder("Notes:\n");
        for (int i = 0; i < max; i++) {
            Note note = notes.get(i);
            response.append("- ")
                    .append(note.getId())
                    .append(" | ")
                    .append(note.summary())
                    .append("\n");
        }
        if (notes.size() > max) {
            response.append("...and ").append(notes.size() - max).append(" more.");
        }
        return response.toString().trim();
    }

    private boolean startsWithAny(String input, String... prefixes) {
        for (String prefix : prefixes) {
            if (input.length() == prefix.length() && input.equalsIgnoreCase(prefix)) {
                return true;
            }
            if (input.length() > prefix.length() && input.regionMatches(true, 0, prefix, 0, prefix.length())
                    && Character.isWhitespace(input.charAt(prefix.length()))) {
                return true;
            }
        }
        return false;
    }

    private String stripKeyword(String input, String... prefixes) {
        for (String prefix : prefixes) {
            if (input.length() == prefix.length() && input.equalsIgnoreCase(prefix)) {
                return "";
            }
            if (input.length() > prefix.length() && input.regionMatches(true, 0, prefix, 0, prefix.length())
                    && Character.isWhitespace(input.charAt(prefix.length()))) {
                return input.substring(prefix.length()).trim();
            }
        }
        return input;
    }
}
