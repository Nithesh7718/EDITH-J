package com.edithj.launcher;

import java.io.IOException;

/**
 * Cross-platform app/URL launcher with OS detection and graceful fallback.
 * Primary support for Windows; falls back to generic Desktop API for other
 * platforms.
 */
public class CrossPlatformLauncher {

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    private static final boolean IS_LINUX = OS_NAME.contains("linux");

    /**
     * Launch an application by name or URL.
     *
     * @param target app name (e.g., "notepad") or URL
     * @return success message or error description
     */
    public String launch(String target) {
        if (target == null || target.isBlank()) {
            return "No target specified.";
        }

        String normalized = target.trim();

        // Try as URL first
        if (isUrl(normalized)) {
            return launchUrl(normalized);
        }

        // Try as app name
        return launchApp(normalized);
    }

    private String launchApp(String appName) {
        try {
            boolean success;

            if (IS_WINDOWS) {
                WindowsLauncher launcher = new WindowsLauncher();
                success = launcher.launch(appName);
            } else if (IS_MAC || IS_LINUX) {
                success = launchUnixApp(appName);
            } else {
                success = launchGenericApp(appName);
            }

            if (success) {
                return "Launched " + appName + ".";
            } else {
                return "Could not find or launch " + appName + ".";
            }
        } catch (IOException exception) {
            return "Error launching " + appName + ": " + exception.getMessage();
        }
    }

    private String launchUrl(String url) {
        try {
            boolean success;

            if (IS_WINDOWS) {
                WindowsLauncher launcher = new WindowsLauncher();
                success = launcher.launchUrl(url);
            } else if (IS_MAC || IS_LINUX) {
                success = launchUnixUrl(url);
            } else {
                success = launchGenericUrl(url);
            }

            if (success) {
                return "Opening " + url + ".";
            } else {
                return "Could not open URL: " + url;
            }
        } catch (IOException exception) {
            return "Error opening URL: " + exception.getMessage();
        }
    }

    private boolean isUrl(String target) {
        return target.startsWith("http://") || target.startsWith("https://");
    }

    private boolean launchGenericApp(String appName) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder(appName);
            pb.start();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean launchGenericUrl(String url) throws IOException {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean launchUnixApp(String appName) throws IOException {
        java.util.Map<String, String> aliases = new java.util.HashMap<>();
        aliases.put("notepad", "gedit");
        aliases.put("calculator", "galculator");
        aliases.put("calc", "galculator");
        aliases.put("text editor", "gedit");
        aliases.put("chrome", "google-chrome");
        aliases.put("firefox", "firefox");

        String resolved = aliases.getOrDefault(appName.toLowerCase(), appName);
        try {
            ProcessBuilder pb = new ProcessBuilder(resolved);
            pb.start();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean launchUnixUrl(String url) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder("xdg-open", url);
            pb.start();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
