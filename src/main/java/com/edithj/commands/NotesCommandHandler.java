package com.edithj.commands;

import com.edithj.assistant.IntentType;

public class NotesCommandHandler implements CommandHandler {

    @Override
    public IntentType intentType() {
        return IntentType.NOTES;
    }

    @Override
    public String handle(CommandContext context) {
        String payload = sanitizePayload(context);
        if (payload.isBlank()) {
            return "Tell me what note to save, for example: note buy milk.";
        }

        if (payload.startsWith("list") || payload.startsWith("show")) {
            return "I can list notes once the NoteService wiring is completed.";
        }

        if (payload.startsWith("delete") || payload.startsWith("remove")) {
            return "I can delete notes once the NoteService wiring is completed.";
        }

        return "Got it. I will save this note after NoteService is wired: \"" + payload + "\".";
    }
}
