package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesktopToolsCommandHandlerTest {

    @Test
    void handle_withScreenshotReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = new DesktopToolsCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("screenshot", "screenshot", "typed"));
        
        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withClipboardReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = new DesktopToolsCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("clipboard", "clipboard", "typed"));
        
        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withoutPayloadReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = new DesktopToolsCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("desktop", "", "typed"));
        
        assertTrue(!response.isBlank());
    }

    @Test
    void intentType_returnsDesktopTools() {
        DesktopToolsCommandHandler handler = new DesktopToolsCommandHandler();
        
        assertTrue(handler.intentType().toString().contains("DESKTOP"));
    }

    @Test
    void handle_withBlankPayloadReturnsFriendlyMessage() {
        DesktopToolsCommandHandler handler = new DesktopToolsCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("desktop", "   ", "typed"));
        
        assertTrue(!response.isBlank());
    }
}

