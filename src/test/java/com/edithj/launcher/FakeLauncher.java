package com.edithj.launcher;

public class FakeLauncher extends AppLauncherService {

    private String lastOpenedTarget = "";
    private String lastOpenedUrl = "";
    private String lastOpenedFile = "";
    private String lastOpenedApp = "";
    private int launchCount;

    @Override
    public String launchApp(String target) {
        String normalized = target == null ? "" : target.trim();
        lastOpenedTarget = normalized;
        launchCount++;

        if (isUrl(normalized)) {
            lastOpenedUrl = normalized;
            return "Opening " + normalized + ".";
        }

        if (isFilePath(normalized)) {
            lastOpenedFile = normalized;
            return "Opening file " + normalized + ".";
        }

        lastOpenedApp = normalized;
        return "Launched " + normalized + ".";
    }

    public String lastOpenedTarget() {
        return lastOpenedTarget;
    }

    public String lastOpenedUrl() {
        return lastOpenedUrl;
    }

    public String lastOpenedFile() {
        return lastOpenedFile;
    }

    public String lastOpenedApp() {
        return lastOpenedApp;
    }

    public int launchCount() {
        return launchCount;
    }

    public void reset() {
        lastOpenedTarget = "";
        lastOpenedUrl = "";
        lastOpenedFile = "";
        lastOpenedApp = "";
        launchCount = 0;
    }

    private boolean isUrl(String target) {
        return target.startsWith("http://")
                || target.startsWith("https://")
                || target.startsWith("mailto:")
                || target.startsWith("file:");
    }

    private boolean isFilePath(String target) {
        return target.contains("/") || target.contains("\\") || target.endsWith(".ics");
    }
}
