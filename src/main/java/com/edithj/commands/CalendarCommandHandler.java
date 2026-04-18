package com.edithj.commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.assistant.IntentType;
import com.edithj.launcher.AppLauncherService;

public class CalendarCommandHandler implements CommandHandler {

    private static final Pattern AM_PM_TIME_PATTERN = Pattern.compile("(?i)\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b");
    private static final Pattern PLAIN_TIME_PATTERN = Pattern.compile("(?i)\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\b");
    private static final Pattern DAY_PATTERN = Pattern.compile("(?i)\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b");
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?i)\\b(?:called|named|for|to)\\s+(.+?)\\s*$");
    private static final DateTimeFormatter ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC);
    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(30);

    private final AppLauncherService launcherService;
    private final Clock clock;

    public CalendarCommandHandler() {
        this(new AppLauncherService(), Clock.systemDefaultZone());
    }

    public CalendarCommandHandler(AppLauncherService launcherService) {
        this(launcherService, Clock.systemDefaultZone());
    }

    public CalendarCommandHandler(AppLauncherService launcherService, Clock clock) {
        this.launcherService = Objects.requireNonNull(launcherService, "launcherService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public IntentType intentType() {
        return IntentType.CALENDAR;
    }

    @Override
    public String handle(CommandContext context) {
        String input = context == null ? "" : context.normalizedInput();
        ParsedCalendarEvent event = parseEvent(input);

        if (!event.isValid()) {
            return "I can draft a calendar event, but I need a title and a time. Try: add a meeting tomorrow at 3pm called project sync.";
        }

        try {
            Path icsFile = writeIcsFile(event);
            String launchResult = launcherService.launchApp(icsFile.toUri().toString());
            ZonedDateTime displayStart = event.startDateTime().atZone(clock.getZone());

            StringBuilder response = new StringBuilder();
            response.append("Opening a calendar draft for ").append(event.title()).append(" on ")
                    .append(displayStart.toLocalDate()).append(" at ")
                    .append(displayStart.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.MINUTES));

            if (launchResult != null && !launchResult.isBlank()) {
                response.append('.').append(' ').append(launchResult.trim());
            } else {
                response.append('.');
            }

            return response.toString().trim();
        } catch (RuntimeException | IOException exception) {
            return genericError();
        }
    }

    ParsedCalendarEvent parseEvent(String rawInput) {
        String normalized = rawInput == null ? "" : rawInput.trim();
        if (normalized.isBlank()) {
            return ParsedCalendarEvent.invalid();
        }

        DateMatch dateMatch = extractDate(normalized);
        TimeMatch timeMatch = extractTime(normalized);
        String title = extractTitle(normalized, dateMatch, timeMatch);

        if (dateMatch.date().isEmpty() || timeMatch.time().isEmpty() || title.isBlank()) {
            return ParsedCalendarEvent.invalid();
        }

        ZonedDateTime start = ZonedDateTime.of(dateMatch.date().get(), timeMatch.time().get(), clock.getZone());
        ZonedDateTime end = start.plus(DEFAULT_DURATION);
        return new ParsedCalendarEvent(title, start.toInstant(), end.toInstant());
    }

    Path writeIcsFile(ParsedCalendarEvent event) throws IOException {
        Path file = Files.createTempFile("edithj-calendar-", ".ics");
        Files.writeString(file, buildIcsContent(event), StandardCharsets.UTF_8);
        return file;
    }

    String buildIcsContent(ParsedCalendarEvent event) {
        String summary = escapeIcsText(event.title());
        String uid = UUID.randomUUID() + "@edithj";
        String dtStamp = ICS_DATE_TIME.format(Instant.now(clock));
        String dtStart = ICS_DATE_TIME.format(event.startDateTime());
        String dtEnd = ICS_DATE_TIME.format(event.endDateTime());

        return String.join("\n",
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//EDITH-J//Calendar Draft//EN",
                "CALSCALE:GREGORIAN",
                "BEGIN:VEVENT",
                "UID:" + uid,
                "DTSTAMP:" + dtStamp,
                "DTSTART:" + dtStart,
                "DTEND:" + dtEnd,
                "SUMMARY:" + summary,
                "END:VEVENT",
                "END:VCALENDAR",
                "");
    }

    private DateMatch extractDate(String input) {
        LocalDate today = LocalDate.now(clock);
        String lower = input.toLowerCase(Locale.ROOT);

        if (lower.contains("tomorrow")) {
            return new DateMatch(Optional.of(today.plusDays(1)), "tomorrow");
        }
        if (lower.contains("today")) {
            return new DateMatch(Optional.of(today), "today");
        }

        Matcher matcher = DAY_PATTERN.matcher(input);
        if (matcher.find()) {
            LocalDate date = today.with(TemporalAdjusters.nextOrSame(parseDayOfWeek(matcher.group(1))));
            return new DateMatch(Optional.of(date), matcher.group(1));
        }

        return DateMatch.empty();
    }

    private TimeMatch extractTime(String input) {
        Matcher amPmMatcher = AM_PM_TIME_PATTERN.matcher(input);
        if (amPmMatcher.find()) {
            int hour = Integer.parseInt(amPmMatcher.group(1));
            int minute = amPmMatcher.group(2) == null ? 0 : Integer.parseInt(amPmMatcher.group(2));
            String suffix = amPmMatcher.group(3).toLowerCase(Locale.ROOT);

            if (hour == 12) {
                hour = 0;
            }
            if ("pm".equals(suffix)) {
                hour += 12;
            }

            return new TimeMatch(Optional.of(LocalTime.of(hour, minute)), amPmMatcher.group());
        }

        Matcher plainMatcher = PLAIN_TIME_PATTERN.matcher(input);
        if (plainMatcher.find()) {
            int hour = Integer.parseInt(plainMatcher.group(1));
            int minute = plainMatcher.group(2) == null ? 0 : Integer.parseInt(plainMatcher.group(2));
            return new TimeMatch(Optional.of(LocalTime.of(hour, minute)), plainMatcher.group());
        }

        return TimeMatch.empty();
    }

    private String extractTitle(String input, DateMatch dateMatch, TimeMatch timeMatch) {
        String working = stripCommandPrefix(input);
        if (!dateMatch.matchedText().isBlank()) {
            working = removePhrase(working, dateMatch.matchedText());
            working = removePhrase(working, "on " + dateMatch.matchedText());
        }
        if (!timeMatch.matchedText().isBlank()) {
            working = removePhrase(working, timeMatch.matchedText());
        }

        Matcher titleMatcher = TITLE_PATTERN.matcher(working);
        if (titleMatcher.find()) {
            return cleanTitle(titleMatcher.group(1));
        }

        working = working.replaceFirst("(?i)^an?\\s+", "");
        working = working.replaceFirst("(?i)^(meeting|event|calendar|appointment|reminder)\\s+", "");
        return cleanTitle(working);
    }

    private String stripCommandPrefix(String input) {
        return input.replaceFirst("(?i)^(add\\s+a\\s+meeting|add\\s+meeting|create\\s+an\\s+event|create\\s+event|schedule\\s+a\\s+meeting|schedule\\s+an\\s+event|schedule\\s+a\\s+reminder|add\\s+event|create\\s+calendar\\s+event|calendar)\\b\\s*", "").trim();
    }

    private String removePhrase(String input, String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return input;
        }
        return input.replaceFirst("(?i)" + Pattern.quote(phrase), " ").replaceAll("\\s+", " ").trim();
    }

    private String cleanTitle(String title) {
        String current = title == null ? "" : title.trim();
        current = current.replaceFirst("(?i)^(called|named|for|to)\\s+", "");
        current = current.replaceAll("(?i)\\b(on|at|today|tomorrow)\\b", "");
        current = current.replaceAll("\\s+", " ").trim();
        return current;
    }

    private DayOfWeek parseDayOfWeek(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "monday" ->
                DayOfWeek.MONDAY;
            case "tuesday" ->
                DayOfWeek.TUESDAY;
            case "wednesday" ->
                DayOfWeek.WEDNESDAY;
            case "thursday" ->
                DayOfWeek.THURSDAY;
            case "friday" ->
                DayOfWeek.FRIDAY;
            case "saturday" ->
                DayOfWeek.SATURDAY;
            default ->
                DayOfWeek.SUNDAY;
        };
    }

    private String escapeIcsText(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    record ParsedCalendarEvent(String title, Instant startDateTime, Instant endDateTime) {

        static ParsedCalendarEvent invalid() {
            return new ParsedCalendarEvent("", Instant.EPOCH, Instant.EPOCH);
        }

        boolean isValid() {
            return title != null && !title.isBlank() && !startDateTime.equals(Instant.EPOCH);
        }
    }

    private record DateMatch(Optional<LocalDate> date, String matchedText) {

        static DateMatch empty() {
            return new DateMatch(Optional.empty(), "");
        }
    }

    private record TimeMatch(Optional<LocalTime> time, String matchedText) {

        static TimeMatch empty() {
            return new TimeMatch(Optional.empty(), "");
        }
    }
}
