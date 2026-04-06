package com.edithj.launcher;

import java.util.Objects;

import com.edithj.util.ValidationUtils;

/**
 * High-level service for safely launching applications and URLs. Provides
 * cross-platform support with graceful error handling.
 *
 * Examples: - launch("notepad") → Opens Notepad - launch("calculator") → Opens
 * Calculator - launch("https://example.com") → Opens URL in default browser
 */
public class AppLauncherService {

    private final CrossPlatformLauncher launcher;

    public AppLauncherService() {
        this.launcher = new CrossPlatformLauncher();
    }

    public AppLauncherService(CrossPlatformLauncher launcher) {
        this.launcher = Objects.requireNonNull(launcher, "launcher cannot be null");
    }

    /**
     * Launch an application by name or open a URL.
     *
     * @param target app name (e.g., "notepad", "calculator") or URL
     * @return user-friendly result message
     */
    public String launchApp(String target) {
        String normalized = ValidationUtils.normalize(target);
        if (ValidationUtils.isEmpty(target)) {
            return "No application or URL specified.";
        }

        return launcher.launch(normalized);
    }

    /**
     * Check if a string is a URL.
     */
    private boolean isUrl(String target) {
        return target.startsWith("http://") || target.startsWith("https://");
    }
}
