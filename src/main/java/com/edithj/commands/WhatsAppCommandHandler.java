package com.edithj.commands;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.assistant.IntentType;
import com.edithj.launcher.AppLauncherService;

public class WhatsAppCommandHandler implements CommandHandler {

    private static final Pattern WHATSAPP_CALL_PATTERN = Pattern.compile(
        "(?i)\\b(?:make\\s+)?(?:a\\s+)?call\\b.*\\b(?:via|on)\\s+whatsapp\\b|\\b(?:via|on)\\s+whatsapp\\b.*\\bcall\\b");
    private static final Pattern CONTACT_PATTERN = Pattern.compile(
            "(?i)\\b(?:message\\s+to|send\\s+to|to)\\s+(.+?)(?=\\s+(?:via\\s+)?whatsapp\\b|[.!?,;:]|$)");
    private static final Pattern CALL_CONTACT_PATTERN = Pattern.compile(
            "(?i)\\bcall\\s+(?:to\\s+)?(.+?)(?=\\s+(?:via|on)\\s+whatsapp\\b|[.!?,;:]|$)");
    private static final Pattern QUOTED_MESSAGE_PATTERN = Pattern.compile("\"([^\"]+)\"|'([^']+)'");

    private final AppLauncherService launcherService;

    public WhatsAppCommandHandler() {
        this(new AppLauncherService());
    }

    public WhatsAppCommandHandler(AppLauncherService launcherService) {
        this.launcherService = Objects.requireNonNull(launcherService, "launcherService");
    }

    @Override
    public IntentType intentType() {
        return IntentType.WHATSAPP;
    }

    @Override
    public String handle(CommandContext context) {
        String input = context == null ? "" : context.normalizedInput();
        ParsedWhatsAppRequest parsedRequest = parseRequest(input);

        if (parsedRequest.callIntent()) {
            String contact = parsedRequest.contactName().isBlank() ? "that contact" : parsedRequest.contactName();
            return "I can't start WhatsApp calls yet, but I can help send a message instead. What should I say to "
                    + contact + "?";
        }

        if (parsedRequest.message().isBlank()) {
            return "I can open WhatsApp with a message, but I didn’t catch the text. Try: send \"hello\" to Krithick via WhatsApp.";
        }

        try {
            String url = buildWhatsAppWebUrl(parsedRequest.message());
            String launchResult = launcherService.launchApp(url);

            StringBuilder response = new StringBuilder();
                response.append("Opening WhatsApp Web with your message")
                    .append(!parsedRequest.contactName().isBlank() ? " to " + parsedRequest.contactName() : "")
                    .append(": \"")
                    .append(parsedRequest.message())
                    .append("\".");
            if (!parsedRequest.contactName().isBlank()) {
                response.append(" I’ll keep the recipient in mind.");
            }

            if (launchResult != null && !launchResult.isBlank()) {
                response.append(' ').append(launchResult.trim());
            }

            return response.toString().trim();
        } catch (RuntimeException exception) {
            return genericError();
        }
    }

    ParsedWhatsAppRequest parseRequest(String rawInput) {
        String normalized = rawInput == null ? "" : rawInput.trim();
        if (normalized.isBlank()) {
            return new ParsedWhatsAppRequest("", "", false);
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

        messageCandidate = messageCandidate.replaceAll("(?i)\\bvia\\s+whatsapp\\b", " ");
        messageCandidate = messageCandidate.replaceAll("(?i)\\bwhatsapp\\b", " ");
        messageCandidate = stripWrapperWords(messageCandidate);

        return new ParsedWhatsAppRequest(messageCandidate, contactName, false);
    }

    String buildWhatsAppWebUrl(String message) {
        String encodedMessage = URLEncoder.encode(message == null ? "" : message, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "https://wa.me/?text=" + encodedMessage;
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
            current = current.replaceAll("(?i)^(send|message|text|please|kindly|whatsapp|via|a|an|the|to|for)\\b[\\s,:-]*", "");
            current = current.replaceAll("(?i)[\\s,:-]*(message|please|whatsapp|via)$", "");
            current = current.replaceAll("\\s+", " ").trim();
        } while (!current.equals(previous));

        return current;
    }

    record ParsedWhatsAppRequest(String message, String contactName, boolean callIntent) {

    }
}
