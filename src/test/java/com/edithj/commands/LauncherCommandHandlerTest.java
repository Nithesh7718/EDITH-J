package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LauncherCommandHandlerTest {

    @Test
    void handle_withApplicationNameReturnsLaunchMessage() {
        LauncherCommandHandler handler = new LauncherCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("launch", "notepad", "typed"));
        
        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withURLReturnsOpenMessage() {
        LauncherCommandHandler handler = new LauncherCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("launch", "https://example.com", "typed"));
        
        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withoutTargetReturnsFriendlyMessage() {
        LauncherCommandHandler handler = new LauncherCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("launch", "", "typed"));
        
        assertTrue(response.toLowerCase().contains("application") || response.toLowerCase().contains("url"));
    }

    @Test
    void intentType_returnsAppLaunch() {
        LauncherCommandHandler handler = new LauncherCommandHandler();
        
        assertTrue(handler.intentType().toString().contains("LAUNCH"));
    }

    @Test
    void handle_withBlankPayloadReturnsFriendlyMessage() {
        LauncherCommandHandler handler = new LauncherCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("launch", "   ", "typed"));
        
        assertTrue(!response.isBlank());
    }
}

