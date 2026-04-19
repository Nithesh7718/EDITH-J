package com.edithj.commands;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.assistant.IntentType;
import com.edithj.launcher.AppLauncherService;

public class EmailCommandHandler implements CommandHandler {

    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern SUBJECT_QUOTED_PATTERN = Pattern.compile(
            "(?i)\\bsubject\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern REGARDING_SUBJECT_QUOTED_PATTERN = Pattern.compile(
            "(?i)\\breg(?:arding)?\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern CONTACT_PATTERN = Pattern.compile(
            "(?i)\\b(?:to|recipient(?:\\s+is)?)\\s+(.+?)(?=\\s+(?:with\\s+)?subject\\b|\\s+(?:and\\s+)?(?:say|saying|message|body)\\b|[.!?,;:]|$)");
    private static final Pattern BODY_SIGNAL_PATTERN = Pattern.compile(
            "(?i)\\b(?:say|saying|message|body)\\s+(.+)$");
    private static final String DEFAULT_SUBJECT = "Message from EDITH-J";

    private final AppLauncherService launcherService;

    public EmailCommandHandler() {
        this(new AppLauncherService());
    }

    public EmailCommandHandler(AppLauncherService launcherService) {
        this.launcherService = Objects.requireNonNull(launcherService, "launcherService");
    }

    @Override
    public IntentType intentType() {
        return IntentType.EMAIL;
    }

    @Override
    public String handle(CommandContext context) {
        String input = context == null ? "" : context.normalizedInput();
        ParsedEmailRequest parsedRequest = parseRequest(input);

        if (parsedRequest.emptySubjectRequested()) {
            return "What should the email be about?";
        }

        if (parsedRequest.body().isBlank() && parsedRequest.subjectExplicit()) {
            String recipient = formatRecipientLabel(parsedRequest);
            return "I’ll draft an email to " + recipient + " with subject \""
                    + parsedRequest.subject() + "\". What would you like the body to say?";
        }

        if (parsedRequest.body().isBlank()) {
            return "I can open your email client with a draft, but I didn’t catch the message text. Try: draft an email to hr@example.com saying I need a day off.";
        }

        try {
            String mailtoUrl = buildMailtoUrl(parsedRequest);
            String launchResult = launcherService.launchApp(mailtoUrl);

            StringBuilder response = new StringBuilder();
            if (!parsedRequest.recipientEmail().isBlank()) {
                response.append("Opening your email client with a draft message to ")
                        .append(parsedRequest.recipientEmail())
                        .append('.');
            } else if (!parsedRequest.contactName().isBlank()) {
                response.append("Opening your email client with a draft message. Please choose the recipient, since I detected '")
                        .append(parsedRequest.contactName())
                        .append("' as the contact name).");
            } else {
                response.append("Opening your email client with a draft message.");
            }

            if (launchResult != null && !launchResult.isBlank()) {
                response.append(' ').append(launchResult.trim());
            }

            return response.toString().trim();
        } catch (RuntimeException exception) {
            return genericError();
        }
    }

    ParsedEmailRequest parseRequest(String rawInput) {
        String normalized = rawInput == null ? "" : rawInput.trim();
        if (normalized.isBlank()) {
            return new ParsedEmailRequest("", "", DEFAULT_SUBJECT, "", false, false);
        }

        String recipientEmail = extractRecipientEmail(normalized);
        String contactName = recipientEmail.isBlank() ? extractContactName(normalized) : "";
        SubjectParseResult subjectParseResult = extractSubject(normalized);
        String subject = subjectParseResult.subject();
        String body = extractBody(normalized, subject, recipientEmail, contactName);

        return new ParsedEmailRequest(
                recipientEmail,
                contactName,
                subject,
                body,
                subjectParseResult.subjectExplicit(),
                subjectParseResult.emptySubjectRequested()
        );
    }

    String buildMailtoUrl(ParsedEmailRequest parsedRequest) {
        StringBuilder mailto = new StringBuilder("mailto:");
        if (!parsedRequest.recipientEmail().isBlank()) {
            mailto.append(parsedRequest.recipientEmail());
        }

        List<String> queryParts = new ArrayList<>();
        if (!parsedRequest.subject().isBlank()) {
            queryParts.add("subject=" + encode(parsedRequest.subject()));
        }
        if (!parsedRequest.body().isBlank()) {
            queryParts.add("body=" + encode(parsedRequest.body()));
        }

        if (!queryParts.isEmpty()) {
            mailto.append('?').append(String.join("&", queryParts));
        }

        return mailto.toString();
    }

    private String extractRecipientEmail(String input) {
        Matcher matcher = EMAIL_ADDRESS_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "";
    }

    private String extractContactName(String input) {
        Matcher matcher = CONTACT_PATTERN.matcher(input);
        if (matcher.find()) {
            String contact = matcher.group(1).trim();
            if (!looksLikeEmail(contact)) {
                return contact;
            }
        }
        return "";
    }

    private SubjectParseResult extractSubject(String input) {
        Matcher regardingMatcher = REGARDING_SUBJECT_QUOTED_PATTERN.matcher(input);
        if (regardingMatcher.find()) {
            String subject = regardingMatcher.group(1);
            String normalizedSubject = subject == null ? "" : subject.trim();
            if (normalizedSubject.isBlank()) {
                return new SubjectParseResult(DEFAULT_SUBJECT, true, true);
            }
            return new SubjectParseResult(normalizedSubject, true, false);
        }

        Matcher quotedMatcher = SUBJECT_QUOTED_PATTERN.matcher(input);
        if (quotedMatcher.find()) {
            return new SubjectParseResult(quotedMatcher.group(1).trim(), true, false);
        }

        Matcher unquotedMatcher = Pattern.compile(
                "(?i)\\bsubject\\s+(.+?)(?=\\s+(?:and\\s+)?(?:say|saying|message|body)\\b|$|[.!?,;:])").matcher(input);
        if (unquotedMatcher.find()) {
            String subject = unquotedMatcher.group(1).trim();
            if (!subject.isBlank()) {
                return new SubjectParseResult(subject, true, false);
            }
        }

        return new SubjectParseResult(DEFAULT_SUBJECT, false, false);
    }

    private String extractBody(String input, String subject, String recipientEmail, String contactName) {
        String body = extractBodyFromSignal(input);
        if (!body.isBlank()) {
            return body;
        }

        String bodyCandidate = stripCommandPrefix(input);
        bodyCandidate = removeSubjectClause(bodyCandidate);
        bodyCandidate = removeRecipientClause(bodyCandidate, recipientEmail, contactName);
        bodyCandidate = stripNoiseWords(bodyCandidate);
        if (!bodyCandidate.isBlank()) {
            return bodyCandidate;
        }

        String quotedBody = extractQuotedBody(input, subject);
        return quotedBody.isBlank() ? "" : quotedBody;
    }

    private String extractBodyFromSignal(String input) {
        Matcher matcher = BODY_SIGNAL_PATTERN.matcher(input);
        if (matcher.find()) {
            String body = matcher.group(1).trim();
            body = removeSubjectClause(body);
            body = stripNoiseWords(body);
            return body;
        }
        return "";
    }

    private String extractQuotedBody(String input, String subject) {
        Matcher matcher = Pattern.compile("\"([^\"]+)\"|'([^']+)'", Pattern.DOTALL).matcher(input);
        while (matcher.find()) {
            String doubleQuoted = matcher.group(1);
            String singleQuoted = matcher.group(2);
            String candidate = doubleQuoted != null ? doubleQuoted.trim() : singleQuoted == null ? "" : singleQuoted.trim();
            if (!candidate.isBlank() && !candidate.equalsIgnoreCase(subject)) {
                return candidate;
            }
        }
        return "";
    }

    private String stripCommandPrefix(String input) {
        String current = input == null ? "" : input.trim();
        current = current.replaceFirst("(?i)^(send\\s+a?n?\\s+email|send\\s+email|draft\\s+a?n?\\s+email|draft\\s+email|compose\\s+a?n?\\s+email|compose\\s+email|write\\s+a?n?\\s+email|write\\s+email|email|mail\\s+to)\\b\\s*", "");
        return current.trim();
    }

    private String removeRecipientClause(String input, String recipientEmail, String contactName) {
        String current = input == null ? "" : input.trim();
        if (!recipientEmail.isBlank()) {
            current = current.replaceFirst("(?i)\\bto\\s+" + Pattern.quote(recipientEmail) + "\\b", "");
        } else if (!contactName.isBlank()) {
            current = current.replaceFirst("(?i)\\bto\\s+" + Pattern.quote(contactName) + "\\b", "");
        }
        return current.trim();
    }

    private String removeSubjectClause(String input) {
        String current = input == null ? "" : input.trim();
        current = current.replaceFirst("(?i)\\bwith\\s+subject\\s*['\"][^'\"]+['\"]", "");
        current = current.replaceFirst("(?i)\\bsubject\\s*['\"][^'\"]+['\"]", "");
        current = current.replaceFirst("(?i)\\breg(?:arding)?\\s*['\"][^'\"]*['\"]", "");
        current = current.replaceFirst("(?i)\\bwith\\s+subject\\s+[^,;:.]+", "");
        current = current.replaceFirst("(?i)\\bsubject\\s+[^,;:.]+", "");
        return current.trim();
    }

    private String formatRecipientLabel(ParsedEmailRequest parsedRequest) {
        if (!parsedRequest.contactName().isBlank()) {
            return parsedRequest.contactName();
        }
        if (!parsedRequest.recipientEmail().isBlank()) {
            return parsedRequest.recipientEmail();
        }
        return "that contact";
    }

    private String stripNoiseWords(String input) {
        String current = input == null ? "" : input.trim();
        String previous;

        do {
            previous = current;
            current = current.replaceFirst("(?i)^(and\\s+)?(?:please\\s+)?(?:say|saying|message|body|with\\s+body)\\b\\s*", "");
            current = current.replaceFirst("(?i)^(and\\s+)?(?:to\\s+)?(?:the\\s+)?(?:recipient|contact)\\b\\s*", "");
            current = current.replaceFirst("(?i)^[,;:\\-\\s]+", "");
            current = current.replaceFirst("(?i)[,;:\\-\\s]+$", "");
            current = current.replaceAll("\\s+", " ").trim();
        } while (!current.equals(previous));

        return current;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean looksLikeEmail(String value) {
        return value != null && EMAIL_ADDRESS_PATTERN.matcher(value.trim()).matches();
    }

    record ParsedEmailRequest(String recipientEmail,
            String contactName,
            String subject,
            String body,
            boolean subjectExplicit,
            boolean emptySubjectRequested) {

    }

    private record SubjectParseResult(String subject, boolean subjectExplicit, boolean emptySubjectRequested) {

    }
}
