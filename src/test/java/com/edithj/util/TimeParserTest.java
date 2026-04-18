package com.edithj.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TimeParserTest {

    @Test
    void parseDuration_parses_seconds() {
        Optional<Duration> duration = TimeParser.parseDuration("30s");
        assertTrue(duration.isPresent());
        assertEquals(Duration.ofSeconds(30), duration.get());
    }

    @Test
    void parseDuration_parses_minutes() {
        Optional<Duration> duration = TimeParser.parseDuration("5m");
        assertTrue(duration.isPresent());
        assertEquals(Duration.ofMinutes(5), duration.get());
    }

    @Test
    void parseDuration_parses_hours() {
        Optional<Duration> duration = TimeParser.parseDuration("2h");
        assertTrue(duration.isPresent());
        assertEquals(Duration.ofHours(2), duration.get());
    }

    @Test
    void parseDuration_parses_days() {
        Optional<Duration> duration = TimeParser.parseDuration("1d");
        assertTrue(duration.isPresent());
        assertEquals(Duration.ofDays(1), duration.get());
    }

    @Test
    void parseDuration_returnsEmptyForInvalidFormat() {
        assertFalse(TimeParser.parseDuration("invalid").isPresent());
        assertFalse(TimeParser.parseDuration("").isPresent());
        assertFalse(TimeParser.parseDuration(null).isPresent());
    }

    @Test
    void parseDuration_returnsEmptyForZero() {
        assertFalse(TimeParser.parseDuration("0s").isPresent());
    }

    @Test
    void parseDuration_returnsEmptyForNegative() {
        assertFalse(TimeParser.parseDuration("-5m").isPresent());
    }

    @Test
    void formatDuration_formats_seconds() {
        String formatted = TimeParser.formatDuration(Duration.ofSeconds(30));
        assertEquals("30s", formatted);
    }

    @Test
    void formatDuration_formats_minutes() {
        String formatted = TimeParser.formatDuration(Duration.ofMinutes(5));
        assertEquals("5m", formatted);
    }

    @Test
    void formatDuration_formats_hours() {
        String formatted = TimeParser.formatDuration(Duration.ofHours(2));
        assertEquals("2h", formatted);
    }

    @Test
    void formatDuration_formats_days() {
        String formatted = TimeParser.formatDuration(Duration.ofDays(1));
        assertEquals("1d", formatted);
    }
}

