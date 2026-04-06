package com.edithj.commands;

import com.edithj.assistant.IntentType;
import com.edithj.launcher.AppLauncherService;

public class LauncherCommandHandler implements CommandHandler {

    private final AppLauncherService launcherService;

    public LauncherCommandHandler() {
        this(new AppLauncherService());
    }

    public LauncherCommandHandler(AppLauncherService launcherService) {
        this.launcherService = launcherService;
    }

    @Override
    public IntentType intentType() {
        return IntentType.APP_LAUNCH;
    }

    @Override
    public String handle(CommandContext context) {
        String payload = sanitizePayload(context);
        if (payload.isBlank()) {
            return "Tell me which app to open, for example: open calculator, or open https://google.com.";
        }

        String appName = normalizeAppName(payload);
        if (appName.isBlank()) {
            return "I could not identify the app name. Try: launch notepad.";
        }

        try {
            return launcherService.launchApp(appName);
        } catch (Exception exception) {
            return genericError();
        }
    }

    private String normalizeAppName(String payload) {
        String appName = payload;
        if (appName.startsWith("app ")) {
            appName = appName.substring(4).trim();
        }
        return appName;
    }
}
