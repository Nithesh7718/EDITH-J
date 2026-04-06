package com.edithj.assistant;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.edithj.commands.CommandHandler;

public class IntentRouter {

    private final Map<IntentType, CommandHandler> handlers = new EnumMap<>(IntentType.class);

    public record RoutedIntent(IntentType intentType, String normalizedInput, String payload) {

    }

    public void registerHandler(CommandHandler handler) {
        if (handler != null) {
            handlers.put(handler.intentType(), handler);
        }
    }

    public AssistantResponse routeAndHandle(String rawInput, String channel) {
        String safeChannel = (channel == null || channel.isBlank()) ? "typed" : channel;
        RoutedIntent routedIntent = route(rawInput);

        CommandHandler handler = handlers.get(routedIntent.intentType());
        String answer;

        if (handler == null) {
            answer = "No handler is configured for intent: " + routedIntent.intentType() + ".";
        } else {
            CommandHandler.CommandContext context = new CommandHandler.CommandContext(
                    routedIntent.normalizedInput(),
                    routedIntent.payload(),
                    safeChannel
            );
            try {
                answer = handler.handle(context);
            } catch (RuntimeException exception) {
                answer = "I could not process that request right now. Please try again.";
            }
        }

        if (answer == null || answer.isBlank()) {
            answer = "I could not complete that request.";
        }

        return new AssistantResponse(routedIntent.intentType(), routedIntent.normalizedInput(), answer.trim(), safeChannel);
    }

    public RoutedIntent route(String rawInput) {
        String normalized = normalize(rawInput);
        if (normalized.isBlank()) {
            return new RoutedIntent(IntentType.FALLBACK_CHAT, "", "");
        }

        IntentType intentType = classify(normalized);
        String payload = extractPayload(normalized, intentType);
        return new RoutedIntent(intentType, normalized, payload);
    }

    private IntentType classify(String normalized) {
        if (containsAny(normalized, "note", "notes", "notepad", "write down", "save this")) {
            return IntentType.NOTES;
        }
        if (containsAny(normalized, "remind", "reminder", "alarm", "schedule", "due")) {
            return IntentType.REMINDERS;
        }
        if (containsAny(normalized, "open", "launch", "start", "run", "execute")) {
            return IntentType.APP_LAUNCH;
        }
        return IntentType.FALLBACK_CHAT;
    }

    private String extractPayload(String normalized, IntentType intentType) {
        return switch (intentType) {
            case APP_LAUNCH ->
                stripLeadingKeyword(normalized, "open", "launch", "start", "run", "execute");
            case NOTES ->
                stripLeadingKeyword(normalized, "note", "notes", "notepad", "write down", "save this");
            case REMINDERS ->
                stripLeadingKeyword(normalized, "remind", "reminder", "alarm", "schedule");
            case FALLBACK_CHAT ->
                normalized;
        };
    }

    private String stripLeadingKeyword(String input, String... keywords) {
        String stripped = input;
        for (String keyword : keywords) {
            if (stripped.startsWith(keyword + " ")) {
                stripped = stripped.substring(keyword.length()).trim();
                break;
            }
            if (stripped.equals(keyword)) {
                return "";
            }
        }
        return stripped;
    }

    private boolean containsAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
