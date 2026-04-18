package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.commands.WhatsAppCommandHandler;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.launcher.AppLauncherService;
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
        RecordingLauncherService launcherService = new RecordingLauncherService();
        service.registerCommandHandler(new WhatsAppCommandHandler(launcherService));

        AssistantResponse response = service.handleTypedInput("whatsapp hello world");

        assertEquals(IntentType.WHATSAPP, response.intentType());
        assertTrue(response.answer().contains("Opening WhatsApp Web with your message"));
        assertEquals("https://wa.me/?text=hello%20world", launcherService.launchedTarget());
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

    private static final class RecordingLauncherService extends AppLauncherService {

        private String launchedTarget = "";

        @Override
        public String launchApp(String target) {
            launchedTarget = target == null ? "" : target;
            return "Launched " + launchedTarget;
        }

        String launchedTarget() {
            return launchedTarget;
        }
    }
}
