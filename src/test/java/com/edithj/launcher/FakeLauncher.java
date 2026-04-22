package com.edithj.launcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    @Override
    public String launchWhatsApp(String message) {
        String normalized = message == null ? "" : message.trim();
        String target = normalized.isBlank()
                ? "whatsapp://send"
                : "whatsapp://send?text=" + URLEncoder.encode(normalized, StandardCharsets.UTF_8).replace("+", "%20");
        return launchApp(target);
    }

    @Override
    public String launchWhatsAppToRecipient(String recipientPhone, String message) {
        String normalizedPhone = recipientPhone == null ? "" : recipientPhone.trim().replaceAll("[^0-9+]", "");
        if (normalizedPhone.startsWith("+")) {
            normalizedPhone = normalizedPhone.substring(1);
        }

        String normalizedMessage = message == null ? "" : message.trim();
        String target = normalizedMessage.isBlank()
                ? "whatsapp://send?phone=" + normalizedPhone
                : "whatsapp://send?phone=" + normalizedPhone + "&text=" + URLEncoder.encode(normalizedMessage, StandardCharsets.UTF_8).replace("+", "%20");
        return launchApp(target);
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
        return target.matches("(?i)^[a-z][a-z0-9+.-]*:.*");
    }

    private boolean isFilePath(String target) {
        return target.contains("/") || target.contains("\\") || target.endsWith(".ics");
    }
}
