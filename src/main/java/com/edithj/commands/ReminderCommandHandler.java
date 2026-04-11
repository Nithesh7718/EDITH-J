package com.edithj.commands;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.edithj.assistant.IntentType;
import com.edithj.reminders.FileReminderRepository;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderService;

public class ReminderCommandHandler implements CommandHandler {

    private final ReminderService reminderService;

    public ReminderCommandHandler() {
        this(new ReminderService(new FileReminderRepository()));
    }

    public ReminderCommandHandler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Override
    public IntentType intentType() {
        return IntentType.REMINDERS;
    }

    @Override
    public String handle(CommandContext context) {
        String payload = sanitizePayload(context);
        if (payload.isBlank()) {
            return "Try one of these: remind me to call mom at 7 PM, list reminders, done <id>, delete reminder <id>.";
        }

        try {
            if (startsWithAny(payload, "list", "show")) {
                return formatReminders(reminderService.listReminders(), "No pending reminders.");
            }

            if (startsWithAny(payload, "snooze")) {
                return handleSnooze(payload);
            }

            if (startsWithAny(payload, "done", "mark done", "completed")) {
                return handleMarkDone(payload);
            }

            if (startsWithAny(payload, "delete", "remove")) {
                return handleDelete(payload);
            }

            if (startsWithAny(payload, "search", "find")) {
                String query = stripKeyword(payload, "search", "find").replaceFirst("(?i)^reminders?\\s*", "").trim();
                if (query.isBlank()) {
                    return "Please provide a search query, for example: search reminders call.";
                }
                return formatReminders(reminderService.searchReminders(query), "No reminders found for: " + query + ".");
            }

            if (startsWithAny(payload, "timer", "set timer")) {
                return handleTimer(payload);
            }

            // Default: create a new reminder
            return handleCreate(payload);

        } catch (Exception exception) {
            return genericError();
        }
    }

    private String handleCreate(String payload) {
        String recurrence = extractRecurrence(payload);

        // Extract time hint (at, on, tomorrow, today, in X minutes/hours)
        String timeHint = extractTimeHint(payload);
        if (timeHint == null && recurrence != null) {
            timeHint = "in " + recurrence;
        }
        if (timeHint == null) {
            return "I need a time hint. Try: remind me to call mom at 7 PM, or in 30 minutes.";
        }

        // Remove time hint and leading "remind me to" for the reminder text
        String text = stripTimeHint(payload, timeHint).replaceFirst("(?i)^(remind\\s+me\\s+to)\\s*", "").trim();
        text = stripKeyword(text, "remind").trim();
        text = text.replaceFirst("(?i)^every\\s+\\d+\\s+(minutes?|hours?|days?)\\s*", "").trim();

        if (text.isBlank()) {
            return "Please tell me what you want to be reminded about.";
        }

        try {
            String finalText = recurrence == null ? text : (text + " [recurs every " + recurrence + "]");
            Reminder reminder = reminderService.createReminder(finalText, timeHint);
            return "Reminder set! \"" + finalText + "\" at " + formatDueTime(reminder.getDueAt());
        } catch (IllegalArgumentException exception) {
            return "I couldn't parse the time. Try: at 5 PM, tomorrow at 9 AM, in 30 minutes.";
        }
    }

    private String handleTimer(String payload) {
        String normalized = payload.toLowerCase();
        Pattern durationPattern = Pattern.compile("(\\d+)\\s*(minutes?|mins?|hours?|hrs?)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = durationPattern.matcher(normalized);
        if (!matcher.find()) {
            return "Use timer like: timer 10 minutes, or set timer 1 hour.";
        }

        int value = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();
        String timeHint = unit.startsWith("h") ? "in " + value + " hours" : "in " + value + " minutes";

        Reminder reminder = reminderService.createReminder("Timer", timeHint);
        return "Timer started for " + value + " " + unit + ". Due at " + formatDueTime(reminder.getDueAt()) + ".";
    }

    private String handleSnooze(String payload) {
        String rest = stripKeyword(payload, "snooze").trim();
        if (rest.isBlank()) {
            return "Use snooze like: snooze <reminder-id> 10 minutes.";
        }

        String[] tokens = rest.split("\\s+");
        String id = tokens[0].trim();
        int amount = 10;
        String unit = "minutes";

        Pattern p = Pattern.compile("(\\d+)\\s*(minutes?|mins?|hours?|hrs?)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = p.matcher(rest.substring(Math.min(rest.length(), id.length())).trim());
        if (matcher.find()) {
            amount = Integer.parseInt(matcher.group(1));
            unit = matcher.group(2).toLowerCase();
        }

        Duration duration = unit.startsWith("h") ? Duration.ofHours(amount) : Duration.ofMinutes(amount);
        Optional<Reminder> snoozed = reminderService.snoozeReminder(id, duration);
        if (snoozed.isEmpty()) {
            return "Could not find reminder with ID: " + id;
        }

        return "Snoozed reminder \"" + snoozed.get().getText() + "\" to " + formatDueTime(snoozed.get().getDueAt()) + ".";
    }

    private String handleMarkDone(String payload) {
        String id = stripKeyword(payload, "done", "mark done", "completed")
                .replaceFirst("(?i)^reminders?\\s*", "")
                .trim();

        if (id.isBlank()) {
            return "Please provide the reminder ID to mark as done.";
        }

        Optional<Reminder> updated = reminderService.markDone(id);
        if (updated.isEmpty()) {
            return "Could not find reminder with ID: " + id;
        }

        return "Reminder marked as done: \"" + updated.get().getText() + "\".";
    }

    private String handleDelete(String payload) {
        String id = stripKeyword(payload, "delete", "remove")
                .replaceFirst("(?i)^reminders?\\s*", "")
                .trim();

        if (id.isBlank()) {
            return "Please provide the reminder ID to delete.";
        }

        boolean deleted = reminderService.deleteReminder(id);
        if (!deleted) {
            return "Could not find reminder with ID: " + id;
        }

        return "Reminder deleted.";
    }

    private String extractTimeHint(String payload) {
        // Look for patterns: "at HH:MM", "at H AM/PM", "tomorrow", "today", "in X minutes/hours"
        if (payload.contains(" at ")) {
            int atIndex = payload.toLowerCase().indexOf(" at ");
            String afterAt = payload.substring(atIndex + 4).trim();
            return afterAt.isBlank() ? null : afterAt;
        }

        if (payload.contains("tomorrow")) {
            return "tomorrow";
        }

        if (payload.contains("today")) {
            return "today";
        }

        if (payload.contains("in ")) {
            int inIndex = payload.toLowerCase().indexOf("in ");
            String afterIn = payload.substring(inIndex).trim();
            return afterIn;
        }

        return null;
    }

    private String extractRecurrence(String payload) {
        java.util.regex.Matcher matcher = Pattern
                .compile("(?i)every\\s+(\\d+)\\s+(minutes?|hours?|days?)")
                .matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + " " + matcher.group(2).toLowerCase();
    }

    private String stripTimeHint(String payload, String timeHint) {
        if (timeHint == null) {
            return payload;
        }

        // Remove the time hint from the payload
        String result = payload.replaceAll("(?i)\\s+at\\s+" + Pattern.quote(timeHint), "");
        result = result.replaceAll("(?i)\\s+tomorrow\\b", "");
        result = result.replaceAll("(?i)\\s+today\\b", "");
        result = result.replaceAll("(?i)\\s+in\\s+\\d+\\s+(minutes?|hours?)\\b", "");
        result = result.replaceAll("(?i)\\s+every\\s+\\d+\\s+(minutes?|hours?|days?)\\b", "");

        return result.trim();
    }

    private String formatReminders(List<Reminder> reminders, String emptyMessage) {
        if (reminders.isEmpty()) {
            return emptyMessage;
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(reminders.size(), 8);

        for (int i = 0; i < limit; i++) {
            Reminder reminder = reminders.get(i);
            String status = reminder.isCompleted() ? "[DONE]" : "[PENDING]";
            sb.append(String.format("%d. %s %s | %s (due: %s)\n",
                    i + 1,
                    status,
                    reminder.getId().substring(0, 8),
                    reminder.summary(),
                    formatDueTime(reminder.getDueAt())));
        }

        if (reminders.size() > limit) {
            sb.append("... and ").append(reminders.size() - limit).append(" more.");
        }

        return sb.toString().trim();
    }

    private String formatDueTime(java.time.Instant instant) {
        if (instant == null) {
            return "unknown";
        }
        java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return ldt.format(fmt);
    }

    private static boolean startsWithAny(String input, String... prefixes) {
        for (String prefix : prefixes) {
            if (input.regionMatches(true, 0, prefix, 0, prefix.length())
                    && (input.length() == prefix.length() || Character.isWhitespace(input.charAt(prefix.length())))) {
                return true;
            }
        }
        return false;
    }

    private static String stripKeyword(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.regionMatches(true, 0, keyword, 0, keyword.length())) {
                int startPos = keyword.length();
                while (startPos < input.length() && Character.isWhitespace(input.charAt(startPos))) {
                    startPos++;
                }
                return input.substring(startPos);
            }
        }
        return input;
    }
}
