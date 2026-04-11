package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FallbackChatHandlerTest {

    @Test
    void handle_returnsTrimmedResponderOutput() {
        FallbackChatHandler handler = new FallbackChatHandler(ctx -> "  hello there  ");

        String response = handler.handle(new CommandHandler.CommandContext("hi", "hi", "typed"));

        assertEquals("hello there", response);
    }

    @Test
    void handle_returnsDefaultWhenResponderEmpty() {
        FallbackChatHandler handler = new FallbackChatHandler(ctx -> "   ");

        String response = handler.handle(new CommandHandler.CommandContext("hi", "hi", "typed"));

        assertEquals("I could not generate a response right now.", response);
    }

    @Test
    void handle_returnsGenericErrorWhenResponderThrows() {
        FallbackChatHandler handler = new FallbackChatHandler(ctx -> {
            throw new RuntimeException("boom");
        });

        String response = handler.handle(new CommandHandler.CommandContext("hi", "hi", "typed"));

        assertEquals("I ran into an issue while handling that request. Please try again.", response);
    }
}
