package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.edithj.launcher.FakeLauncher;

class CalendarCommandHandlerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-18T10:15:00Z"), ZoneOffset.UTC);

    @Test
    void handle_createsIcsForTomorrowMeeting() throws Exception {
        FakeLauncher launcherService = new FakeLauncher();
        CalendarCommandHandler handler = new CalendarCommandHandler(launcherService, FIXED_CLOCK);

        String response = handler.handle(new CommandHandler.CommandContext(
                "add a meeting tomorrow at 3pm called project sync",
                "add a meeting tomorrow at 3pm called project sync",
                "typed"));

        assertTrue(response.contains("Opening a calendar draft for project sync"));
        assertTrue(launcherService.lastOpenedUrl().endsWith(".ics"));

        String ics = Files.readString(Path.of(java.net.URI.create(launcherService.lastOpenedUrl())), StandardCharsets.UTF_8);
        assertTrue(ics.contains("SUMMARY:project sync"));
        assertTrue(ics.contains("DTSTART:20260419T150000Z"));
        assertTrue(ics.contains("DTEND:20260419T153000Z"));
    }

    @Test
    void handle_createsIcsForNamedDayEvent() throws Exception {
        FakeLauncher launcherService = new FakeLauncher();
        CalendarCommandHandler handler = new CalendarCommandHandler(launcherService, FIXED_CLOCK);

        String response = handler.handle(new CommandHandler.CommandContext(
                "create an event on Monday at 10 for standup",
                "create an event on Monday at 10 for standup",
                "typed"));

        assertTrue(response.contains("Opening a calendar draft for standup"));
        String ics = Files.readString(Path.of(java.net.URI.create(launcherService.lastOpenedUrl())), StandardCharsets.UTF_8);
        assertTrue(ics.contains("SUMMARY:standup"));
        assertTrue(ics.contains("DTSTART:20260420T100000Z"));
        assertTrue(ics.contains("DTEND:20260420T103000Z"));
    }

    @Test
    void handle_createsIcsForReminderStyleRequest() throws Exception {
        FakeLauncher launcherService = new FakeLauncher();
        CalendarCommandHandler handler = new CalendarCommandHandler(launcherService, FIXED_CLOCK);

        String response = handler.handle(new CommandHandler.CommandContext(
                "schedule a reminder on Friday at 5pm to call mom",
                "schedule a reminder on Friday at 5pm to call mom",
                "typed"));

        assertTrue(response.contains("Opening a calendar draft for call mom"));
        String ics = Files.readString(Path.of(java.net.URI.create(launcherService.lastOpenedUrl())), StandardCharsets.UTF_8);
        assertTrue(ics.contains("SUMMARY:call mom"));
        assertTrue(ics.contains("DTSTART:20260424T170000Z"));
        assertTrue(ics.contains("DTEND:20260424T173000Z"));
    }

    @Test
    void handle_returnsClarificationWhenInputMissingData() {
        FakeLauncher launcherService = new FakeLauncher();
        CalendarCommandHandler handler = new CalendarCommandHandler(launcherService, FIXED_CLOCK);

        String response = handler.handle(new CommandHandler.CommandContext("calendar", "calendar", "typed"));

        assertTrue(response.contains("I can draft a calendar event"));
        assertEquals("", launcherService.lastOpenedTarget());
    }
}
