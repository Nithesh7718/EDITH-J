package com.edithj.commands;

import java.util.Objects;
import java.util.function.Function;

import com.edithj.assistant.IntentType;

public class FallbackChatHandler implements CommandHandler {

    private final Function<CommandContext, String> chatResponder;

    public FallbackChatHandler(Function<CommandContext, String> chatResponder) {
        this.chatResponder = Objects.requireNonNull(chatResponder, "chatResponder");
    }

    @Override
    public IntentType intentType() {
        return IntentType.FALLBACK_CHAT;
    }

    @Override
    public String handle(CommandContext context) {
        try {
            String response = chatResponder.apply(context);
            if (response == null || response.isBlank()) {
                return "I could not generate a response right now.";
            }
            return response.trim();
        } catch (RuntimeException exception) {
            return genericError();
        }
    }
}
