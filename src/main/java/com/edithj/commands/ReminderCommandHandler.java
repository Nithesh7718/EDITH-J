package com.edithj.commands;

import com.edithj.assistant.IntentType;

public class ReminderCommandHandler implements CommandHandler {

    @Override
    public IntentType intentType() {
        return IntentType.REMINDERS;
    }

    @Override
    public String handle(CommandContext context) {
        String payload = sanitizePayload(context);
        if (payload.isBlank()) {
            return "Tell me what to remind you about and when, for example: remind me to call mom at 7 PM.";
        }

        if (payload.startsWith("list") || payload.startsWith("show")) {
            return "I can list reminders once ReminderService is wired.";
        }

        if (!(payload.contains(" at ") || payload.contains(" on ") || payload.contains(" tomorrow") || payload.contains(" today"))) {
            return "I need a time hint. Try: remind me to submit report at 5 PM.";
        }

        return "Reminder captured. I will schedule it after ReminderService wiring: \"" + payload + "\".";
    }
}
