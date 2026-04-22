package com.edithj.launcher;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.ui.session.UiPreferencesService;
import com.edithj.util.ValidationUtils;

/**
 * High-level service for safely launching applications and URLs. Provides
 * cross-platform support with graceful error handling.
 *
 * Examples: - launch("notepad") → Opens Notepad - launch("calculator") → Opens
 * Calculator - launch("https://example.com") → Opens URL in default browser
 */
public class AppLauncherService {

    private static final Pattern WHATSAPP_PROTOCOL_PATTERN = Pattern.compile(
            "(?i)^whatsapp://send(?:\\?text=([^&]*))?.*$");

    private static final String YOUTUBE_WEB_URL = "https://www.youtube.com";
    private static final String WHATSAPP_WEB_URL = "https://web.whatsapp.com/";
    private static final String WHATSAPP_WEB_MESSAGE_URL = "https://wa.me/?text=";

    private final CrossPlatformLauncher launcher;
    private final UiPreferencesService preferences;

    public AppLauncherService() {
        this(new CrossPlatformLauncher(), UiPreferencesService.instance());
    }

    public AppLauncherService(CrossPlatformLauncher launcher) {
        this(launcher, UiPreferencesService.instance());
    }

    public AppLauncherService(CrossPlatformLauncher launcher, UiPreferencesService preferences) {
        this.launcher = Objects.requireNonNull(launcher, "launcher cannot be null");
        this.preferences = Objects.requireNonNull(preferences, "preferences cannot be null");
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

        String webFallback = resolveWebFallback(normalized);
        if (webFallback != null && !preferences.isPreferShortcutAppsEnabled()) {
            return launcher.launch(webFallback);
        }

        String launchResult = launcher.launch(normalized);
        if (!isLaunchFailure(launchResult)) {
            return launchResult;
        }

        if (webFallback != null && preferences.isWebFallbackAllowed()) {
            String fallbackResult = launcher.launch(webFallback);
            if (!isLaunchFailure(fallbackResult)) {
                return fallbackResult;
            }
            return fallbackResult;
        }

        return launchResult;
    }

    public String launchWhatsApp(String message) {
        String normalizedMessage = ValidationUtils.normalize(message);

        if (preferences.isWhatsAppAppFirstEnabled()) {
            String appTarget = normalizedMessage.isBlank()
                    ? "whatsapp://send"
                    : "whatsapp://send?text=" + encodeForUri(normalizedMessage);

            String appResult = launcher.launch(appTarget);
            if (!isLaunchFailure(appResult)) {
                return appResult;
            }

            if (!preferences.isWebFallbackAllowed()) {
                return appResult;
            }

            String webTarget = normalizedMessage.isBlank()
                    ? WHATSAPP_WEB_URL
                    : WHATSAPP_WEB_MESSAGE_URL + encodeForUri(normalizedMessage);
            String webResult = launcher.launch(webTarget);
            if (!isLaunchFailure(webResult)) {
                return normalizedMessage.isBlank()
                        ? "Opened WhatsApp Web."
                        : "Opened WhatsApp Web with your message.";
            }
            return webResult;
        }

        String webTarget = normalizedMessage.isBlank()
                ? WHATSAPP_WEB_URL
                : WHATSAPP_WEB_MESSAGE_URL + encodeForUri(normalizedMessage);
        return launcher.launch(webTarget);
    }

    public String launchWhatsAppToRecipient(String recipientPhone, String message) {
        String normalizedPhone = normalizePhone(recipientPhone);
        if (normalizedPhone.isBlank()) {
            return launchWhatsApp(message);
        }

        String normalizedMessage = ValidationUtils.normalize(message);
        if (preferences.isWhatsAppAppFirstEnabled()) {
            String appTarget = buildWhatsAppAppRecipientTarget(normalizedPhone, normalizedMessage);
            String appResult = launcher.launch(appTarget);
            if (!isLaunchFailure(appResult)) {
                return appResult;
            }

            if (!preferences.isWebFallbackAllowed()) {
                return appResult;
            }

            String webTarget = buildWhatsAppWebRecipientTarget(normalizedPhone, normalizedMessage);
            String webResult = launcher.launch(webTarget);
            if (!isLaunchFailure(webResult)) {
                return normalizedMessage.isBlank()
                        ? "Opened WhatsApp Web chat."
                        : "Opened WhatsApp Web chat with your message.";
            }
            return webResult;
        }

        return launcher.launch(buildWhatsAppWebRecipientTarget(normalizedPhone, normalizedMessage));
    }

    private boolean isLaunchFailure(String result) {
        if (result == null || result.isBlank()) {
            return true;
        }

        String normalized = result.trim().toLowerCase();
        return normalized.startsWith("could not") || normalized.startsWith("error");
    }

    private String resolveWebFallback(String normalized) {
        String lower = normalized.toLowerCase();
        if (lower.equals("youtube") || lower.equals("youtube.com") || lower.equals("open youtube")) {
            return YOUTUBE_WEB_URL;
        }

        if (lower.equals("whatsapp") || lower.startsWith("whatsapp://")) {
            Matcher matcher = WHATSAPP_PROTOCOL_PATTERN.matcher(normalized);
            if (matcher.matches()) {
                String encodedMessage = matcher.group(1);
                if (encodedMessage == null || encodedMessage.isBlank()) {
                    return WHATSAPP_WEB_URL;
                }
                return WHATSAPP_WEB_MESSAGE_URL + encodedMessage;
            }
            return WHATSAPP_WEB_URL;
        }

        return null;
    }

    private String encodeForUri(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String normalizePhone(String recipientPhone) {
        String raw = ValidationUtils.normalize(recipientPhone);
        if (raw.isBlank()) {
            return "";
        }

        String normalized = raw.replaceAll("[^0-9+]", "");
        if (normalized.startsWith("+")) {
            return "+" + normalized.substring(1).replaceAll("[^0-9]", "");
        }
        return normalized.replaceAll("[^0-9]", "");
    }

    private String buildWhatsAppAppRecipientTarget(String normalizedPhone, String normalizedMessage) {
        String phoneParam = normalizedPhone.startsWith("+") ? normalizedPhone.substring(1) : normalizedPhone;
        if (normalizedMessage.isBlank()) {
            return "whatsapp://send?phone=" + phoneParam;
        }
        return "whatsapp://send?phone=" + phoneParam + "&text=" + encodeForUri(normalizedMessage);
    }

    private String buildWhatsAppWebRecipientTarget(String normalizedPhone, String normalizedMessage) {
        String phonePath = normalizedPhone.startsWith("+") ? normalizedPhone.substring(1) : normalizedPhone;
        if (normalizedMessage.isBlank()) {
            return "https://wa.me/" + phonePath;
        }
        return "https://wa.me/" + phonePath + "?text=" + encodeForUri(normalizedMessage);
    }

}
