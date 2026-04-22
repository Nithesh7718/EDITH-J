package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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
    void route_classifiesWhatsAppCommands() {
        IntentRouter router = new IntentRouter();

        IntentRouter.RoutedIntent routed = router.route("send a \"hello\" message to Krithick via whatsapp");

        assertEquals(IntentType.WHATSAPP, routed.intentType());
        assertTrue(routed.normalizedInput().contains("whatsapp"));
    }

    @Test
    void route_classifiesWhatsAppTypoCommands() {
        IntentRouter router = new IntentRouter();

        IntentRouter.RoutedIntent routed = router.route("open whtsapp and send hi to krithick");

        assertEquals(IntentType.WHATSAPP, routed.intentType());
    }

    @Test
    void route_classifiesEmailCommands() {
        IntentRouter router = new IntentRouter();

        IntentRouter.RoutedIntent routed = router.route("draft an email to hr@example.com with subject 'Leave request' and say I need a day off");

        assertEquals(IntentType.EMAIL, routed.intentType());
        assertTrue(routed.normalizedInput().contains("email"));
    }

    @Test
    void route_classifiesCalendarCommands() {
        IntentRouter router = new IntentRouter();

        IntentRouter.RoutedIntent routed = router.route("add a meeting tomorrow at 3pm called project sync");

        assertEquals(IntentType.CALENDAR, routed.intentType());
        assertTrue(routed.payload().contains("tomorrow"));
    }

    @Test
    void routeAndHandle_invokesRegisteredEmailHandler() {
        IntentRouter router = new IntentRouter();
        RecordingHandler handler = new RecordingHandler(IntentType.EMAIL, "opened");
        router.registerHandler(handler);

        AssistantResponse response = router.routeAndHandle("email hello to Krithick", "typed");

        assertEquals(IntentType.EMAIL, response.intentType());
        assertEquals("opened", response.answer());
        assertNotNull(handler.lastContext());
        assertEquals("email hello to Krithick", handler.lastContext().normalizedInput());
        assertEquals("hello to Krithick", handler.lastContext().payload());
        assertEquals("typed", handler.lastContext().channel());
    }

    @Test
    void routeAndHandle_invokesRegisteredCalendarHandler() {
        IntentRouter router = new IntentRouter();
        RecordingHandler handler = new RecordingHandler(IntentType.CALENDAR, "drafted");
        router.registerHandler(handler);

        AssistantResponse response = router.routeAndHandle("add a meeting tomorrow at 3pm called project sync", "typed");

        assertEquals(IntentType.CALENDAR, response.intentType());
        assertEquals("drafted", response.answer());
        assertNotNull(handler.lastContext());
        assertEquals("add a meeting tomorrow at 3pm called project sync", handler.lastContext().normalizedInput());
        assertTrue(handler.lastContext().payload().contains("tomorrow"));
        assertEquals("typed", handler.lastContext().channel());
    }

    @Test
    void routeAndHandle_invokesRegisteredWhatsAppHandler() {
        IntentRouter router = new IntentRouter();
        RecordingHandler handler = new RecordingHandler(IntentType.WHATSAPP, "opened");
        router.registerHandler(handler);

        AssistantResponse response = router.routeAndHandle("whatsapp hello", "typed");

        assertEquals(IntentType.WHATSAPP, response.intentType());
        assertEquals("opened", response.answer());
        assertNotNull(handler.lastContext());
        assertEquals("whatsapp hello", handler.lastContext().normalizedInput());
        assertEquals("whatsapp hello", handler.lastContext().payload());
        assertEquals("typed", handler.lastContext().channel());
    }

    @Test
    void routeAndHandle_invokesRegisteredHandlerWithNormalizedChannel() {
        IntentRouter router = new IntentRouter();
        RecordingHandler handler = new RecordingHandler(IntentType.NOTES, "saved");
        router.registerHandler(handler);

        AssistantResponse response = router.routeAndHandle(" note buy milk ", "");

        assertEquals(IntentType.NOTES, response.intentType());
        assertEquals("typed", response.channel());
        assertEquals("saved", response.answer());
        assertNotNull(handler.lastContext());
        assertEquals("note buy milk", handler.lastContext().normalizedInput());
        assertEquals("buy milk", handler.lastContext().payload());
        assertEquals("typed", handler.lastContext().channel());
    }

    @Test
    void routeAndHandle_returnsFallbackMessageWhenHandlerThrows() {
        IntentRouter router = new IntentRouter();
        RecordingHandler handler = new RecordingHandler(IntentType.NOTES, new RuntimeException("boom"));
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

    private static final class RecordingHandler implements CommandHandler {

        private final IntentType intentType;
        private final String response;
        private final RuntimeException failure;
        private CommandContext lastContext;

        RecordingHandler(IntentType intentType, String response) {
            this.intentType = intentType;
            this.response = response;
            this.failure = null;
        }

        RecordingHandler(IntentType intentType, RuntimeException failure) {
            this.intentType = intentType;
            this.response = null;
            this.failure = failure;
        }

        @Override
        public IntentType intentType() {
            return intentType;
        }

        @Override
        public String handle(CommandContext context) {
            lastContext = context;
            if (failure != null) {
                throw failure;
            }
            return response;
        }

        CommandContext lastContext() {
            return lastContext;
        }
    }
}
