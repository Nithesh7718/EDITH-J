package com.edithj.commands;

import com.edithj.assistant.IntentType;

public interface CommandHandler {

    record CommandContext(String normalizedInput, String payload, String channel) {

    }

    IntentType intentType();

    String handle(CommandContext context);

    default String sanitizePayload(CommandContext context) {
        if (context == null || context.payload() == null) {
            return "";
        }
        return context.payload().trim();
    }

    default String genericError() {
        return "I ran into an issue while handling that request. Please try again.";
    }
}
