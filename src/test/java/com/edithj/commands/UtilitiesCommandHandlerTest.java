package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UtilitiesCommandHandlerTest {

    @Test
    void handle_withCalculationReturnsResult() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("calculate", "2 + 2", "typed"));
        
        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withTimeConversionReturnsResult() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("time", "utc", "typed"));
        
        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withoutPayloadReturnsFriendlyMessage() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("utilities", "", "typed"));
        
        assertTrue(!response.isBlank());
    }

    @Test
    void intentType_returnsUtilities() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        
        assertTrue(handler.intentType().toString().contains("UTILITIES"));
    }

    @Test
    void handle_withBlankPayloadReturnsFriendlyMessage() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("utilities", "   ", "typed"));
        
        assertTrue(!response.isBlank());
    }
}

