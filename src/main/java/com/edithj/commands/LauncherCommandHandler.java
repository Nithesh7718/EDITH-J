package com.edithj.commands;

import com.edithj.assistant.IntentType;

public class LauncherCommandHandler implements CommandHandler {

    @Override
    public IntentType intentType() {
        return IntentType.APP_LAUNCH;
    }

    @Override
    public String handle(CommandContext context) {
        String payload = sanitizePayload(context);
        if (payload.isBlank()) {
            return "Tell me which app to open, for example: open calculator.";
        }

        String appName = normalizeAppName(payload);
        if (appName.isBlank()) {
            return "I could not identify the app name. Try: launch notepad.";
        }

        return "I will launch \"" + appName + "\" once AppLauncherService is wired.";
    }

    private String normalizeAppName(String payload) {
        String appName = payload;
        if (appName.startsWith("app ")) {
            appName = appName.substring(4).trim();
        }
        return appName;
    }
}
