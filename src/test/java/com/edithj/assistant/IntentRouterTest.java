package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edithj.commands.CommandHandler;

class IntentRouterTest {

    @Test
    void route_classifiesNotesAndExtractsPayload() {
        IntentRouter router = new IntentRouter();
        IntentRouter.RoutedIntent routed = router.route("note buy milk and eggs");

        assertEquals(IntentType.NOTES, routed.intentType());
        assertEquals("buy milk and eggs", routed.payload());
    }

    @Test
    void route_classifiesMathAsUtilities() {
        IntentRouter router = new IntentRouter();
        IntentRouter.RoutedIntent routed = router.route("calculate 12 * (3 + 1)");

        assertEquals(IntentType.UTILITIES, routed.intentType());
        assertTrue(routed.payload().contains("12"));
    }

    @Test
    void routeAndHandle_invokesRegisteredHandlerWithNormalizedChannel() {
        IntentRouter router = new IntentRouter();
        CommandHandler handler = mock(CommandHandler.class);
        when(handler.intentType()).thenReturn(IntentType.NOTES);
        when(handler.handle(org.mockito.ArgumentMatchers.any())).thenReturn("saved");
        router.registerHandler(handler);

        AssistantResponse response = router.routeAndHandle(" note buy milk ", "");

        assertEquals(IntentType.NOTES, response.intentType());
        assertEquals("typed", response.channel());
        assertEquals("saved", response.answer());
        verify(handler).handle(org.mockito.ArgumentMatchers.argThat(ctx
                -> "note buy milk".equals(ctx.normalizedInput())
                && "buy milk".equals(ctx.payload())
                && "typed".equals(ctx.channel())));
    }

    @Test
    void routeAndHandle_returnsFallbackMessageWhenHandlerThrows() {
        IntentRouter router = new IntentRouter();
        CommandHandler handler = mock(CommandHandler.class);
        when(handler.intentType()).thenReturn(IntentType.NOTES);
        when(handler.handle(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("boom"));
        router.registerHandler(handler);

        AssistantResponse response = router.routeAndHandle("note task", "typed");

        assertEquals("I could not process that request right now. Please try again.", response.answer());
    }

    @Test
    void routeAndHandle_usesDefaultMessageWhenHandlerMissing() {
        IntentRouter router = new IntentRouter();
        AssistantResponse response = router.routeAndHandle("open calculator", "typed");

        assertEquals(IntentType.APP_LAUNCH, response.intentType());
        assertTrue(response.answer().startsWith("No handler is configured for intent:"));
    }
}
