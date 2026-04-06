package com.edithj.launcher;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Windows-specific app launcher using ProcessBuilder and common Windows app
 * paths. Supports built-in apps, program files directories, and direct
 * executables.
 */
public class WindowsLauncher {

    // Map of common app names to Windows executable names or paths
    private static final Map<String, String> APPS = new HashMap<>();

    static {
        // System apps (usually in System32)
        APPS.put("notepad", "notepad.exe");
        APPS.put("calc", "calc.exe");
        APPS.put("calculator", "calc.exe");
        APPS.put("paint", "mspaint.exe");
        APPS.put("wordpad", "wordpad.exe");
        APPS.put("task manager", "taskmgr.exe");
        APPS.put("explorer", "explorer.exe");
        APPS.put("cmd", "cmd.exe");
        APPS.put("powershell", "powershell.exe");
        APPS.put("settings", "ms-settings:");
        APPS.put("clock", "ms-clock:");
        APPS.put("weather", "ms-weather:");
        APPS.put("mail", "outlook.exe");
        APPS.put("calendar", "outlookcal.exe");

        // Browser shortcuts
        APPS.put("chrome", "chrome.exe");
        APPS.put("firefox", "firefox.exe");
        APPS.put("edge", "msedge.exe");
        APPS.put("internet explorer", "iexplore.exe");
    }

    private static final String[] COMMON_PATHS = {
        System.getenv("ProgramFiles"),
        System.getenv("ProgramFiles(x86)"),
        "C:\\Program Files",
        "C:\\Program Files (x86)",
        System.getenv("AppData") + "\\Microsoft\\Windows\\Start Menu\\Programs"
    };

    /**
     * Resolve and launch an app by name. Tries known apps first, then searches
     * common program paths.
     *
     * @param appName the app name
     * @return true if launch succeeded, false otherwise
     */
    public boolean launch(String appName) {
        if (appName == null || appName.isBlank()) {
            return false;
        }

        String normalized = appName.toLowerCase().trim();

        try {
            // Check if it's a known app
            String exe = APPS.get(normalized);
            if (exe != null) {
                return tryLaunch(exe);
            }

            // Try direct executable name
            if (tryLaunch(normalized)) {
                return true;
            }

            // Search in common paths
            String executable = findExecutable(normalized);
            if (executable != null) {
                return tryLaunch(executable);
            }

            return false;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Open a URL in the default browser.
     */
    public boolean launchUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            // Use cmd /c start to open URL in default application
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", url);
            pb.start();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Try to launch an executable. Supports: - Direct commands (calc.exe,
     * notepad.exe) - Registry URLs (ms-settings:, ms-clock:) - Full paths
     */
    private boolean tryLaunch(String executable) throws IOException {
        if (executable.startsWith("ms-")) {
            // Handle registry (ms-settings:, ms-clock:, etc.)
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", executable);
                pb.start();
                return true;
            } catch (IOException exception) {
                return false;
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(executable);
            pb.start();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Search for an executable in common program directories.
     */
    private String findExecutable(String appName) {
        String exeName = appName.contains(".") ? appName : appName + ".exe";

        for (String basePath : COMMON_PATHS) {
            if (basePath == null || basePath.isBlank()) {
                continue;
            }

            File path = new File(basePath);
            if (!path.exists()) {
                continue;
            }

            // Search one level deep
            File[] dirs = path.listFiles(File::isDirectory);
            if (dirs == null) {
                continue;
            }

            for (File dir : dirs) {
                File exe = new File(dir, exeName);
                if (exe.exists() && exe.canExecute()) {
                    return exe.getAbsolutePath();
                }
            }
        }

        return null;
    }
}
