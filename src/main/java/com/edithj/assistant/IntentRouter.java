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

    private IntentType classify(String input) {
        if (containsAnyIgnoreCase(input, "note", "notes", "notepad", "write down", "save this")) {
            return IntentType.NOTES;
        }
        if (isCalendarCommand(input)) {
            return IntentType.CALENDAR;
        }
        if (containsAnyIgnoreCase(input, "remind", "reminder", "alarm", "schedule", "due", "snooze")
                || startsWithAnyIgnoreCase(input, "timer", "set timer")) {
            return IntentType.REMINDERS;
        }
        if (isEmailCommand(input)) {
            return IntentType.EMAIL;
        }
        if (containsAnyIgnoreCase(input, "whatsapp")) {
            return IntentType.WHATSAPP;
        }
        if (isExplicitDesktopCommand(input)) {
            return IntentType.DESKTOP_TOOLS;
        }
        if (containsAnyIgnoreCase(input, "open", "launch", "start", "run", "execute")) {
            return IntentType.APP_LAUNCH;
        }
        if (containsAnyIgnoreCase(input, "weather", "forecast", "temperature", "rain")) {
            return IntentType.WEATHER;
        }
        if (containsAnyIgnoreCase(input,
                "time", "date", "day", "calculate", "calc", "plus", "minus", "multiply", "divide")
                || input.matches(".*\\d+\\s*[+\\-*/()]\\s*\\d+.*")) {
            return IntentType.UTILITIES;
        }
        return IntentType.FALLBACK_CHAT;
    }

    private boolean isCalendarCommand(String input) {
        return startsWithAnyIgnoreCase(input,
                "add a meeting", "add meeting", "create an event", "create event", "schedule a meeting",
                "schedule an event", "schedule a reminder", "calendar", "add event", "create calendar event")
                || containsAnyIgnoreCase(input, "meeting", "event", "calendar", "appointment")
                && containsAnyIgnoreCase(input, "today", "tomorrow", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "at ", "called", "named", "for ", "to ");
    }

    private boolean isExplicitDesktopCommand(String input) {
        return startsWithAnyIgnoreCase(input,
                "help", "capabilities", "what can you do", "system info", "device info", "memory status",
                "search web", "google", "browse", "open website", "open site", "good morning", "daily briefing",
                "show clipboard", "read clipboard", "save clipboard as note", "copy ", "draft email", "write email", "compose email",
                "find file", "open file", "open downloads", "recent files", "add task", "todo add", "list tasks", "show tasks",
                "done task", "remove task", "delete task", "start focus", "focus status", "end focus", "block site", "unblock site",
                "blocked sites", "start work mode", "shutdown work mode", "work mode", "action log", "confirm open", "what did you do");
    }

    private String extractPayload(String input, IntentType intentType) {
        return switch (intentType) {
            case APP_LAUNCH ->
                stripLeadingKeywordIgnoreCase(input, "open", "launch", "start", "run", "execute");
            case EMAIL ->
                stripLeadingKeywordIgnoreCase(input, "email", "send an email", "send email", "draft an email", "draft email", "compose an email", "compose email", "write an email", "write email", "mail to");
            case CALENDAR ->
                stripLeadingKeywordIgnoreCase(input, "add a meeting", "add meeting", "create an event", "create event", "schedule a meeting", "schedule an event", "schedule a reminder", "calendar", "add event", "create calendar event");
            case WHATSAPP ->
                input;
            case NOTES ->
                stripLeadingKeywordIgnoreCase(input, "note", "notes", "notepad", "write down", "save this");
            case REMINDERS ->
                stripLeadingKeywordIgnoreCase(input, "remind", "reminder", "alarm", "schedule");
            case WEATHER ->
                stripLeadingKeywordIgnoreCase(input, "weather", "forecast", "temperature", "rain", "check weather", "check forecast");
            case UTILITIES ->
                stripLeadingKeywordIgnoreCase(input, "calculate", "calc", "what is", "what's", "time", "date", "day");
            case DESKTOP_TOOLS ->
                stripLeadingKeywordIgnoreCase(input, "help", "search web", "google", "browse", "open website", "open site", "system info", "device info", "memory status");
            case FALLBACK_CHAT ->
                input;
        };
    }

    private String stripLeadingKeywordIgnoreCase(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.length() == keyword.length() && input.equalsIgnoreCase(keyword)) {
                return "";
            }
            if (input.length() > keyword.length() && input.regionMatches(true, 0, keyword, 0, keyword.length())
                    && Character.isWhitespace(input.charAt(keyword.length()))) {
                return input.substring(keyword.length()).trim();
            }
        }
        return input;
    }

    private boolean containsAnyIgnoreCase(String input, String... keywords) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmailCommand(String input) {
        String lower = input.toLowerCase(Locale.ROOT).trim();
        return lower.matches(".*\\b(email|mail)\\b.*")
                || startsWithAnyIgnoreCase(lower, "send an email", "send email", "draft an email", "draft email", "compose an email", "compose email", "write an email", "write email", "email", "mail to")
                || lower.contains(" with subject ") && lower.contains(" email ");
    }

    private boolean startsWithAnyIgnoreCase(String input, String... prefixes) {
        String normalized = input.toLowerCase(Locale.ROOT).trim();
        for (String prefix : prefixes) {
            String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
            if (normalized.startsWith(normalizedPrefix)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim();
    }
}
