package com.edithj.commands;

import com.edithj.assistant.IntentType;
import com.edithj.launcher.AppLauncherService;
import com.edithj.launcher.AppNameResolver;

public class LauncherCommandHandler implements CommandHandler {

    private final AppLauncherService launcherService;
    private final AppNameResolver appNameResolver;

    public LauncherCommandHandler() {
        this(new AppLauncherService(), new AppNameResolver());
    }

    public LauncherCommandHandler(AppLauncherService launcherService) {
        this(launcherService, new AppNameResolver());
    }

    public LauncherCommandHandler(AppLauncherService launcherService, AppNameResolver appNameResolver) {
        this.launcherService = launcherService;
        this.appNameResolver = appNameResolver;
    }

    @Override
    public IntentType intentType() {
        return IntentType.APP_LAUNCH;
    }

    @Override
    public String handle(CommandContext context) {
        String payload = sanitizePayload(context);
        if (payload.isBlank()) {
            return "Please specify an application to open. For example: open calculator, or open URL https://google.com.";
        }

        String appName = normalizeAppName(payload);
        if (appName.isBlank()) {
            return "I could not identify the app name. Try: launch notepad.";
        }

        if (!isLikelyUrl(appName)) {
            String resolvedApp = appNameResolver.resolveLaunchTarget(appName);
            if (!resolvedApp.isBlank()) {
                appName = resolvedApp;
            }
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

    private boolean isLikelyUrl(String value) {
        return value != null && value.matches("(?i)^[a-z][a-z0-9+.-]*:.*");
    }
}
