package com.edithj.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        if (hint == null || hint.isBlank()) {
            return null;
        }

        hint = hint.toLowerCase().trim();
        LocalDateTime now = LocalDateTime.now();

        // Handle "in X minutes/hours" pattern
        Matcher minutesMatcher = MINUTES_PATTERN.matcher(hint);
        if (minutesMatcher.find()) {
            int minutes = Integer.parseInt(minutesMatcher.group(1));
            return now.plusMinutes(minutes).atZone(ZoneId.systemDefault()).toInstant();
        }

        Matcher hoursMatcher = HOURS_PATTERN.matcher(hint);
        if (hoursMatcher.find()) {
            int hours = Integer.parseInt(hoursMatcher.group(1));
            return now.plusHours(hours).atZone(ZoneId.systemDefault()).toInstant();
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

            return targetTime.atZone(ZoneId.systemDefault()).toInstant();
        }

        return null;
    }
}
