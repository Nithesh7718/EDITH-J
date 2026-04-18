package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.edithj.launcher.FakeLauncher;

class LauncherCommandHandlerTest {

    @Test
    void handle_withApplicationNameReturnsLaunchMessage() {
        FakeLauncher fakeLauncher = new FakeLauncher();
        LauncherCommandHandler handler = new LauncherCommandHandler(fakeLauncher);
        String response = handler.handle(new CommandHandler.CommandContext("launch", "notepad", "typed"));

        assertTrue(response != null && !response.isBlank(), "Response should not be null or blank");
        assertEquals("notepad", fakeLauncher.lastOpenedApp());
    }

    @Test
    void handle_withURLReturnsOpenMessage() {
        FakeLauncher fakeLauncher = new FakeLauncher();
        LauncherCommandHandler handler = new LauncherCommandHandler(fakeLauncher);
        String response = handler.handle(new CommandHandler.CommandContext("launch", "https://example.com", "typed"));

        assertTrue(response != null && !response.isBlank(), "Response should not be null or blank");
        assertEquals("https://example.com", fakeLauncher.lastOpenedUrl());
    }

    @Test
    void handle_withoutTargetReturnsFriendlyMessage() {
        FakeLauncher fakeLauncher = new FakeLauncher();
        LauncherCommandHandler handler = new LauncherCommandHandler(fakeLauncher);
        String response = handler.handle(new CommandHandler.CommandContext("launch", "", "typed"));

        assertTrue(response.toLowerCase().contains("application") || response.toLowerCase().contains("url"));
        assertEquals(0, fakeLauncher.launchCount());
    }

    @Test
    void intentType_returnsAppLaunch() {
        LauncherCommandHandler handler = new LauncherCommandHandler(new FakeLauncher());

        assertTrue(handler.intentType().toString().contains("LAUNCH"));
    }

    @Test
    void handle_withBlankPayloadReturnsFriendlyMessage() {
        FakeLauncher fakeLauncher = new FakeLauncher();
        LauncherCommandHandler handler = new LauncherCommandHandler(fakeLauncher);
        String response = handler.handle(new CommandHandler.CommandContext("launch", "   ", "typed"));

        assertTrue(!response.isBlank());
        assertEquals(0, fakeLauncher.launchCount());
    }
}
