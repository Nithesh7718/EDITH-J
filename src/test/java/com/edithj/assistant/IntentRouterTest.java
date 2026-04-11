package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.edithj.commands.CommandHandler;

class IntentRouterTest {

    private IntentRouter router;

    @BeforeEach
    void setUp() {
        router = new IntentRouter();
    }

    @Test
    void route_classifiesNotesAndExtractsPayload() {
        IntentRouter.RoutedIntent routed = router.route("note buy milk and eggs");

        assertEquals(IntentType.NOTES, routed.intentType());
        assertEquals("buy milk and eggs", routed.payload());
    }

    @Test
    void route_classifiesMathAsUtilities() {
        IntentRouter.RoutedIntent routed = router.route("calculate 12 * (3 + 1)");

        assertEquals(IntentType.UTILITIES, routed.intentType());
        assertTrue(routed.payload().contains("12"));
    }

    @Test
    void routeAndHandle_invokesRegisteredHandlerWithNormalizedChannel() {
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
        CommandHandler handler = mock(CommandHandler.class);
        when(handler.intentType()).thenReturn(IntentType.NOTES);
        when(handler.handle(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("boom"));
        router.registerHandler(handler);

        AssistantResponse response = router.routeAndHandle("note task", "typed");

        assertEquals("I could not process that request right now. Please try again.", response.answer());
    }

    @Test
    void routeAndHandle_usesDefaultMessageWhenHandlerMissing() {
        AssistantResponse response = router.routeAndHandle("open calculator", "typed");

        assertEquals(IntentType.APP_LAUNCH, response.intentType());
        assertTrue(response.answer().startsWith("No handler is configured for intent:"));
    }
}
