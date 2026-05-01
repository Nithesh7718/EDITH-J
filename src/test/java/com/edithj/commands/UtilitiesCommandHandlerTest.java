package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class UtilitiesCommandHandlerTest {

    private CommandHandler.CommandContext context(String input) {
        return new CommandHandler.CommandContext(input, input, "typed");
    }

    @Test
    void handle_withCalculationReturnsResult() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        String response = handler.handle(context("2 + 2"));

        assertEquals("Result: 4", response);
    }

    @Test
    void handle_withTimeConversionReturnsResult() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        String response = handler.handle(context("what time is it"));

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
        String response = handler.handle(new CommandHandler.CommandContext("   ", "   ", "typed"));

        assertTrue(!response.isBlank());
    }

    @Test
    void handle_withInvalidCalculationReturnsGuidance() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        String response = handler.handle(context("calculate 2 +"));

        assertTrue(response.contains("couldn't parse"));
    }

    @Test
    void handle_withDivisionByZeroReturnsSpecificMessage() {
        UtilitiesCommandHandler handler = new UtilitiesCommandHandler();
        String response = handler.handle(context("calculate 10 / 0"));

        assertEquals("Cannot divide by zero. Try a different calculation.", response);
    }
}
