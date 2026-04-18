package com.edithj.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing natural language time hints into Instant values. Supports
 * formats like "5 PM", "tomorrow at 9 AM", "in 30 minutes", etc.
 */
public final class TimeParser {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|AM|PM)?");
    private static final Pattern MINUTES_PATTERN = Pattern.compile("in\\s+(\\d+)\\s+minutes?", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS_PATTERN = Pattern.compile("in\\s+(\\d+)\\s+hours?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURATION_PATTERN = Pattern.compile("^(-?\\d+)\\s*([smhd])$", Pattern.CASE_INSENSITIVE);

    private TimeParser() {
        // Utility class
    }

    /**
     * Parse time hints like "5 PM", "tomorrow at 9 AM", "today at 3 PM", "in 30
     * minutes"
     *
     * @param hint the time hint string
     * @return parsed Instant, or null if unable to parse
     */
    public static Instant parseTimeHint(String hint) {
        return parseTimeHint(hint, Clock.systemDefaultZone());
    }

    public static Instant parseTimeHint(String hint, Clock clock) {
        if (hint == null || hint.isBlank()) {
            return null;
        }

        hint = hint.toLowerCase().trim();
        LocalDateTime now = LocalDateTime.now(clock);

        // Handle "in X minutes/hours" pattern
        Matcher minutesMatcher = MINUTES_PATTERN.matcher(hint);
        if (minutesMatcher.find()) {
            int minutes = Integer.parseInt(minutesMatcher.group(1));
            return now.plusMinutes(minutes).atZone(clock.getZone()).toInstant();
        }

        Matcher hoursMatcher = HOURS_PATTERN.matcher(hint);
        if (hoursMatcher.find()) {
            int hours = Integer.parseInt(hoursMatcher.group(1));
            return now.plusHours(hours).atZone(clock.getZone()).toInstant();
        }

        // Determine the day: today, tomorrow, or within the string
        LocalDateTime targetTime = now;
        if (hint.contains("tomorrow")) {
            targetTime = now.plusDays(1);
        }

        // Extract time from hint
        Matcher timeMatcher = TIME_PATTERN.matcher(hint);
        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            int minute = timeMatcher.group(2) != null ? Integer.parseInt(timeMatcher.group(2)) : 0;
            String meridiem = timeMatcher.group(3);

            // Convert 12-hour to 24-hour if AM/PM provided
            if (meridiem != null) {
                if ("pm".equals(meridiem) && hour != 12) {
                    hour += 12;
                } else if ("am".equals(meridiem) && hour == 12) {
                    hour = 0;
                }
            }

            // Ensure hour is valid
            if (hour < 0 || hour > 23) {
                return null;
            }

            targetTime = targetTime.withHour(hour).withMinute(minute).withSecond(0);

            // If the parsed time is in the past and no explicit day was mentioned, move to next day
            if (!hint.contains("today") && !hint.contains("tomorrow") && targetTime.isBefore(now)) {
                targetTime = targetTime.plusDays(1);
            }

            return targetTime.atZone(clock.getZone()).toInstant();
        }

        return null;
    }

    public static Optional<Duration> parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = DURATION_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }

        if (amount <= 0) {
            return Optional.empty();
        }

        Duration duration = switch (matcher.group(2).toLowerCase()) {
            case "s" ->
                Duration.ofSeconds(amount);
            case "m" ->
                Duration.ofMinutes(amount);
            case "h" ->
                Duration.ofHours(amount);
            case "d" ->
                Duration.ofDays(amount);
            default ->
                Duration.ZERO;
        };

        return duration.isZero() ? Optional.empty() : Optional.of(duration);
    }

    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration");
        }

        if (duration.toDays() > 0 && duration.equals(Duration.ofDays(duration.toDays()))) {
            return duration.toDays() + "d";
        }
        if (duration.toHours() > 0 && duration.equals(Duration.ofHours(duration.toHours()))) {
            return duration.toHours() + "h";
        }
        if (duration.toMinutes() > 0 && duration.equals(Duration.ofMinutes(duration.toMinutes()))) {
            return duration.toMinutes() + "m";
        }
        return duration.getSeconds() + "s";
    }
}
