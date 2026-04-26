package com.edithj.commands;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.assistant.IntentType;
import com.edithj.config.AppConfig;
import com.edithj.launcher.AppLauncherService;

public class WhatsAppCommandHandler implements CommandHandler {

    private static final String CONTACT_PROPERTY_PREFIX = "edith.whatsapp.contact.";

    private static final String WHATSAPP_ALIAS_PATTERN = "(?:whatsapp|whtsapp|whatsap|watsapp|whats\\s*app)";

    private static final Pattern OPEN_WHATSAPP_PATTERN = Pattern.compile(
            "(?i)^(?:open|launch|start|run)\\s+" + WHATSAPP_ALIAS_PATTERN + "(?:\\s+app)?\\s*$|^" + WHATSAPP_ALIAS_PATTERN + "\\s*$");
    private static final Pattern OPEN_WHATSAPP_AND_SEND_PATTERN = Pattern.compile(
            "(?i)^(?:open|launch|start|run)\\s+" + WHATSAPP_ALIAS_PATTERN + "(?:\\s+app)?\\s+(?:and\\s+)?send\\s+(.+)$");

    private static final Pattern WHATSAPP_CALL_PATTERN = Pattern.compile(
            "(?i)\\b(?:make\\s+)?(?:a\\s+)?call\\b.*\\b(?:via|on)\\s+" + WHATSAPP_ALIAS_PATTERN + "\\b|\\b(?:via|on)\\s+" + WHATSAPP_ALIAS_PATTERN + "\\b.*\\bcall\\b");
    private static final Pattern CONTACT_PATTERN = Pattern.compile(
            "(?i)\\b(?:message\\s+to|send\\s+to|to)\\s+(.+?)(?=\\s+(?:(?:via|on)\\s+)?" + WHATSAPP_ALIAS_PATTERN + "\\b|[.!?,;:]|$)");
    private static final Pattern CALL_CONTACT_PATTERN = Pattern.compile(
            "(?i)\\bcall\\s+(?:to\\s+)?(.+?)(?=\\s+(?:via|on)\\s+" + WHATSAPP_ALIAS_PATTERN + "\\b|[.!?,;:]|$)");
    private static final Pattern QUOTED_MESSAGE_PATTERN = Pattern.compile("\"([^\"]+)\"|'([^']+)'");

    private final AppLauncherService launcherService;
    private final Properties appProperties;

    public WhatsAppCommandHandler() {
        this(new AppLauncherService(), AppConfig.load().properties());
    }

    public WhatsAppCommandHandler(AppLauncherService launcherService) {
        this(launcherService, AppConfig.load().properties());
    }

    public WhatsAppCommandHandler(AppLauncherService launcherService, Properties appProperties) {
        this.launcherService = Objects.requireNonNull(launcherService, "launcherService");
        this.appProperties = Objects.requireNonNull(appProperties, "appProperties");
    }

    @Override
    public IntentType intentType() {
        return IntentType.WHATSAPP;
    }

    @Override
    public String handle(CommandContext context) {
        String input = context == null ? "" : context.normalizedInput();

        if (isOpenWhatsAppRequest(input)) {
            return openWhatsAppApp();
        }

        ParsedWhatsAppRequest parsedRequest = parseRequest(input);

        if (parsedRequest.callIntent()) {
            String contact = parsedRequest.contactName().isBlank() ? "that contact" : parsedRequest.contactName();
            return "I can't start WhatsApp calls yet, but I can help send a message instead. What should I say to "
                    + contact + "?";
        }

        if (parsedRequest.message().isBlank()) {
            return openWhatsAppApp();
        }

        try {
            String recipientPhone = resolveMappedPhone(parsedRequest.contactName());

            // If not in contacts, check if the contact name itself IS a phone number
            if (recipientPhone.isBlank() && looksLikePhoneNumber(parsedRequest.contactName())) {
                recipientPhone = parsedRequest.contactName().replaceAll("[^0-9+]", "");
                // Ensure Indian numbers without country code get +91 prefixed
                if (!recipientPhone.startsWith("+") && recipientPhone.length() == 10) {
                    recipientPhone = "+91" + recipientPhone;
                }
            }

            boolean mappedRecipient = !recipientPhone.isBlank();

            String launchResult = mappedRecipient
                    ? launcherService.launchWhatsAppToRecipient(recipientPhone, parsedRequest.message())
                    : launcherService.launchWhatsApp(parsedRequest.message());
            boolean openedInWeb = launchResult != null && launchResult.toLowerCase().contains("web");

            StringBuilder response = new StringBuilder();
            response.append(openedInWeb
                    ? "Opening WhatsApp Web with your message"
                    : "Opening WhatsApp in the app with your message")
                    .append(!parsedRequest.contactName().isBlank() ? " to " + parsedRequest.contactName() : "")
                    .append(": \"")
                    .append(parsedRequest.message())
                    .append("\".");
            if (!parsedRequest.contactName().isBlank() && !mappedRecipient) {
                response.append(" I couldn't auto-select that recipient yet. Add mapping in edith.properties as ")
                        .append(CONTACT_PROPERTY_PREFIX)
                        .append(normalizeContactKey(parsedRequest.contactName()))
                        .append("=+<countrycode><number>.");
            }

            if (launchResult != null) {
                String normalizedLaunchResult = launchResult.trim().toLowerCase();
                if (normalizedLaunchResult.startsWith("could not") || normalizedLaunchResult.startsWith("error")) {
                    response.append(' ').append(launchResult.trim());
                }
            }

            return response.toString().trim();
        } catch (RuntimeException exception) {
            return genericError();
        }
    }

    private boolean isOpenWhatsAppRequest(String input) {
        String normalized = input == null ? "" : input.trim();
        return OPEN_WHATSAPP_PATTERN.matcher(normalized).matches();
    }

    private String openWhatsAppApp() {
        String launchResult = launcherService.launchWhatsApp("");
        if (launchResult == null || launchResult.isBlank()) {
            return "Opening WhatsApp.";
        }

        String normalized = launchResult.trim().toLowerCase();
        if (normalized.contains("web")) {
            return "Opening WhatsApp Web.";
        }
        if (normalized.startsWith("could not") || normalized.startsWith("error")) {
            return launchResult.trim();
        }
        return "Opening WhatsApp.";
    }

    ParsedWhatsAppRequest parseRequest(String rawInput) {
        String normalized = rawInput == null ? "" : rawInput.trim();
        if (normalized.isBlank()) {
            return new ParsedWhatsAppRequest("", "", false);
        }

        Matcher openAndSendMatcher = OPEN_WHATSAPP_AND_SEND_PATTERN.matcher(normalized);
        if (openAndSendMatcher.matches()) {
            normalized = openAndSendMatcher.group(1).trim();
        }

        boolean callIntent = WHATSAPP_CALL_PATTERN.matcher(normalized).find();

        String contactName = callIntent ? extractCallContactName(normalized) : extractContactName(normalized);
        String quotedMessage = extractQuotedMessage(normalized);
        if (!quotedMessage.isBlank()) {
            return new ParsedWhatsAppRequest(quotedMessage, contactName, callIntent);
        }

        if (callIntent) {
            return new ParsedWhatsAppRequest("", contactName, true);
        }

        String messageCandidate = normalized;
        if (!contactName.isBlank()) {
            Matcher contactMatcher = CONTACT_PATTERN.matcher(normalized);
            if (contactMatcher.find()) {
                messageCandidate = messageCandidate.replace(contactMatcher.group(0), " ");
            }
        }

        messageCandidate = messageCandidate.replaceAll("(?i)\\bvia\\s+" + WHATSAPP_ALIAS_PATTERN + "\\b", " ");
        messageCandidate = messageCandidate.replaceAll("(?i)\\b" + WHATSAPP_ALIAS_PATTERN + "\\b", " ");
        messageCandidate = stripWrapperWords(messageCandidate);

        return new ParsedWhatsAppRequest(messageCandidate, contactName, false);
    }

    String buildWhatsAppAppTarget(String message) {
        String encodedMessage = URLEncoder.encode(message == null ? "" : message, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return encodedMessage.isBlank() ? "whatsapp://send" : "whatsapp://send?text=" + encodedMessage;
    }

    private String extractContactName(String input) {
        Matcher matcher = CONTACT_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String extractQuotedMessage(String input) {
        Matcher matcher = QUOTED_MESSAGE_PATTERN.matcher(input);
        if (matcher.find()) {
            String doubleQuoted = matcher.group(1);
            if (doubleQuoted != null && !doubleQuoted.isBlank()) {
                return doubleQuoted.trim();
            }

            String singleQuoted = matcher.group(2);
            if (singleQuoted != null && !singleQuoted.isBlank()) {
                return singleQuoted.trim();
            }
        }
        return "";
    }

    private String extractCallContactName(String input) {
        Matcher matcher = CALL_CONTACT_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return extractContactName(input);
    }

    private String stripWrapperWords(String text) {
        String current = text == null ? "" : text.trim();
        String previous;

        do {
            previous = current;
            current = current.replaceAll("(?i)^(and|open|launch|start|run|send|sen|snd|message|msg|text|just|jusdt|please|kindly|whatsapp|whtsapp|whatsap|watsapp|via|a|an|the|to|for)\\b[\\s,:-]*", "");
            current = current.replaceAll("(?i)[\\s,:-]*(message|please|whatsapp|whtsapp|whatsap|watsapp|via|on)$", "");
            current = current.replaceAll("\\s+", " ").trim();
        } while (!current.equals(previous));

        return current;
    }

    private String resolveMappedPhone(String contactName) {
        if (contactName == null || contactName.isBlank()) {
            return "";
        }

        String contactKey = normalizeContactKey(contactName);
        if (contactKey.isBlank()) {
            return "";
        }

        String mapped = appProperties.getProperty(CONTACT_PROPERTY_PREFIX + contactKey, "").trim();
        if (mapped.isBlank()) {
            return "";
        }

        return mapped;
    }

    private boolean looksLikePhoneNumber(String contactName) {
        if (contactName == null || contactName.isBlank()) {
            return false;
        }
        // Strip spaces, dashes, and parens — what remains must be only digits and optionally a leading +
        String stripped = contactName.replaceAll("[\\s\\-().+]", "");
        return stripped.matches("[0-9]{7,15}");
    }

    private String normalizeContactKey(String contactName) {
        return contactName == null
                ? ""
                : contactName.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    record ParsedWhatsAppRequest(String message, String contactName, boolean callIntent) {

    }
}
