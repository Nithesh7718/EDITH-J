package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.commands.CalendarCommandHandler;
import com.edithj.commands.EmailCommandHandler;
import com.edithj.commands.WhatsAppCommandHandler;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.launcher.FakeLauncher;
import com.edithj.speech.SpeechRecognizer;
import com.edithj.speech.SpeechService;
import com.edithj.speech.TypedFallbackService;

class AssistantServiceTest {

    @Test
    void handleTypedInput_routesStructuredIntentsWithoutFallbackChat() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, 12);

        AssistantResponse response = service.handleTypedInput("note buy milk");

        assertEquals(IntentType.NOTES, response.intentType());
        assertNotNull(response.answer());
        assertFalse(response.answer().isBlank());
        assertFalse(fallbackChatService.wasInvoked());
    }

    @Test
    void handleTypedInput_routesWhatsAppIntentsWithoutFallbackChat() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, 12);
        FakeLauncher launcherService = new FakeLauncher();
        service.registerCommandHandler(new WhatsAppCommandHandler(launcherService));

        AssistantResponse response = service.handleTypedInput("whatsapp hello world");

        assertEquals(IntentType.WHATSAPP, response.intentType());
        assertTrue(response.answer().contains("Opening WhatsApp in the app with your message"));
        assertEquals("whatsapp://send?text=hello%20world", launcherService.lastOpenedUrl());
        assertFalse(fallbackChatService.wasInvoked());
    }

    @Test
    void handleTypedInput_recoversWhatsappFollowUpTypoAfterWhatsappContext() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, 12);
        FakeLauncher launcherService = new FakeLauncher();
        service.registerCommandHandler(new WhatsAppCommandHandler(launcherService));

        AssistantResponse first = service.handleTypedInput("open whtsapp");
        AssistantResponse second = service.handleTypedInput("sen HI to krithick");
        AssistantResponse third = service.handleTypedInput("jusdt send hello to krithick");

        assertEquals(IntentType.WHATSAPP, first.intentType());
        assertEquals(IntentType.WHATSAPP, second.intentType());
        assertEquals(IntentType.WHATSAPP, third.intentType());
        assertTrue(second.answer().contains("Opening WhatsApp in the app with your message"));
        assertTrue(third.answer().contains("Opening WhatsApp in the app with your message"));
        assertTrue(launcherService.lastOpenedUrl().startsWith("whatsapp://send?text=hello"));
        assertFalse(fallbackChatService.wasInvoked());
    }

    @Test
    void handleTypedInput_routesEmailIntentsWithoutFallbackChat() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, 12);
        FakeLauncher launcherService = new FakeLauncher();
        service.registerCommandHandler(new EmailCommandHandler(launcherService));

        AssistantResponse response = service.handleTypedInput("email hello to Krithick");

        assertEquals(IntentType.EMAIL, response.intentType());
        assertTrue(response.answer().contains("Opening your email client with a draft message"));
        assertEquals("mailto:?subject=Message%20from%20EDITH-J&body=hello", launcherService.lastOpenedUrl());
        assertFalse(fallbackChatService.wasInvoked());
    }

    @Test
    void handleTypedInput_routesCalendarIntentsWithoutFallbackChat() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, 12);
        FakeLauncher launcherService = new FakeLauncher();
        service.registerCommandHandler(new CalendarCommandHandler(launcherService, java.time.Clock.fixed(java.time.Instant.parse("2026-04-18T10:15:00Z"), java.time.ZoneOffset.UTC)));

        AssistantResponse response = service.handleTypedInput("add a meeting tomorrow at 3pm called project sync");

        assertEquals(IntentType.CALENDAR, response.intentType());
        assertTrue(response.answer().contains("Opening a calendar draft for project sync"));
        assertTrue(launcherService.lastOpenedUrl().endsWith(".ics"));
        assertFalse(fallbackChatService.wasInvoked());
    }

    private static final class TestPromptBuilder extends PromptBuilder {

        @Override
        public String loadSystemPrompt() {
            return "";
        }
    }

    private static final class TrackingFallbackChatService extends FallbackChatService {

        private boolean invoked;

        TrackingFallbackChatService(LlmClient llmClient, PromptBuilder promptBuilder, int maxTurns) {
            super(llmClient, promptBuilder, maxTurns);
        }

        @Override
        public String runFallbackChat(String channel) {
            invoked = true;
            return super.runFallbackChat(channel);
        }

        boolean wasInvoked() {
            return invoked;
        }
    }

}
