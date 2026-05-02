package com.edithj.assistant;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.edithj.commands.CalendarCommandHandler;
import com.edithj.commands.EmailCommandHandler;
import com.edithj.commands.ReminderCommandHandler;
import com.edithj.commands.WhatsAppCommandHandler;
import com.edithj.config.PreferencesService;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.launcher.FakeLauncher;
import com.edithj.reminders.InMemoryReminderRepository;
import com.edithj.reminders.ReminderService;
import com.edithj.speech.SpeechRecognizer;
import com.edithj.speech.SpeechService;
import com.edithj.speech.TypedFallbackService;

class AssistantServiceTest {

    @BeforeEach
    @AfterEach
    @SuppressWarnings("unused")
    void clearPersistedAssistantState() {
        PreferencesService.instance().clearPendingApproval();
        PreferencesService.instance().clearActivePlan();
    }

    @Test
    void handleTypedInput_routesStructuredIntentsWithoutFallbackChat() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);

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

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);
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

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);
        FakeLauncher launcherService = new FakeLauncher();
        service.registerCommandHandler(new WhatsAppCommandHandler(launcherService, new Properties()));

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

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);
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

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);
        FakeLauncher launcherService = new FakeLauncher();
        service.registerCommandHandler(new CalendarCommandHandler(launcherService, java.time.Clock.fixed(java.time.Instant.parse("2026-04-18T10:15:00Z"), java.time.ZoneOffset.UTC)));

        AssistantResponse response = service.handleTypedInput("add a meeting tomorrow at 3pm called project sync");

        assertEquals(IntentType.CALENDAR, response.intentType());
        assertTrue(response.answer().contains("Opening a calendar draft for project sync"));
        assertTrue(launcherService.lastOpenedUrl().endsWith(".ics"));
        assertFalse(fallbackChatService.wasInvoked());
    }

    @Test
    void handleTypedInput_returnsClarificationForLowConfidenceAmbiguousInput() {
        AssistantTelemetry.instance().reset();
        LlmClient llmClient = prompt -> "non-json llm output";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);

        AssistantResponse response = service.handleTypedInput("open notes in our project");

        assertEquals(IntentType.GENERAL_CHAT, response.intentType());
        assertTrue(response.answer().contains("Did you mean"));
        assertFalse(fallbackChatService.wasInvoked());
        assertEquals(1L, AssistantTelemetry.instance().snapshot().clarificationPrompts());
    }

    @Test
    void handleTypedInput_requiresApprovalForRiskyDesktopAutomation() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);

        AssistantResponse response = service.handleTypedInput("file move notes.txt to archive/notes.txt");

        assertEquals(IntentType.DESKTOP_AUTOMATION, response.intentType());
        assertTrue(response.requiresApproval());
        assertEquals("file_move", response.approvalType());
        assertTrue(response.answer().contains("Approval required"));
        assertFalse(response.actions().isEmpty());
        assertTrue(PreferencesService.instance().getPendingApproval() != null);
    }

    @Test
    void handleTypedInput_approveFollowUpExecutesPendingDesktopAutomation() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);

        AssistantResponse pending = service.handleTypedInput("file move notes.txt to archive/notes.txt");
        AssistantResponse approved = service.handleTypedInput("approve last action");

        assertTrue(pending.requiresApproval());
        assertFalse(approved.requiresApproval());
        assertEquals(IntentType.DESKTOP_AUTOMATION, approved.intentType());
        assertTrue(approved.answer().contains("Source file does not exist") || approved.answer().contains("Moved file"));
        assertNull(PreferencesService.instance().getPendingApproval());
    }

    @Test
    void handleTypedInput_failedDesktopAutomationGetsRecoveryOptions() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);

        service.handleTypedInput("file move notes.txt to archive/notes.txt");
        AssistantResponse approved = service.handleTypedInput("approve last action");

        assertFalse(approved.success());
        assertFalse(approved.recoveryOptions().isEmpty());
        assertFalse(approved.explanation().isBlank());
    }

    @Test
    void assistantService_restoresPendingApprovalFromPreferences() {
        PreferencesService.instance().savePendingApproval("file move notes.txt to archive/notes.txt", "typed", "file_move");

        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);
        AssistantResponse approved = service.handleTypedInput("approve last action");

        assertEquals(IntentType.DESKTOP_AUTOMATION, approved.intentType());
        assertFalse(approved.requiresApproval());
        assertNull(PreferencesService.instance().getPendingApproval());
    }

    @Test
    void handleTypedInput_buildsTaskPlanForCompoundRequest() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);

        AssistantResponse response = service.handleTypedInput("create a reminder, draft an email, and open the calendar for tomorrow");

        assertEquals(IntentType.GENERAL_CHAT, response.intentType());
        assertEquals("Planner", response.source());
        assertNotNull(response.taskPlan());
        assertTrue(response.taskPlan().steps().size() >= 2);
        assertFalse(response.actions().isEmpty());
    }

    @Test
    void handleTypedInput_canStartAndContinuePlan() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);
        InMemoryReminderRepository reminderRepository = new InMemoryReminderRepository();
        FakeLauncher launcherService = new FakeLauncher();
        service.registerCommandHandler(new ReminderCommandHandler(new ReminderService(reminderRepository)));
        service.registerCommandHandler(new EmailCommandHandler(launcherService));
        service.registerCommandHandler(new CalendarCommandHandler(launcherService, java.time.Clock.fixed(java.time.Instant.parse("2026-04-18T10:15:00Z"), java.time.ZoneOffset.UTC)));

        AssistantResponse planned = service.handleTypedInput("create a reminder, draft an email, and open the calendar for tomorrow");
        AssistantResponse started = service.handleTypedInput("start this plan");
        AssistantResponse continued = service.handleTypedInput("continue this plan");
        String afterContinueUrl = launcherService.lastOpenedUrl();
        AssistantResponse finished = service.handleTypedInput("continue this plan");

        assertNotNull(planned.taskPlan());
        assertEquals("done", started.taskPlan().steps().get(0).status());
        assertTrue(started.success());
        assertEquals(1, reminderRepository.findAll().size());
        assertEquals("done", continued.taskPlan().steps().get(1).status());
        assertTrue(afterContinueUrl.startsWith("mailto:"));
        assertEquals("done", finished.taskPlan().steps().get(2).status());
        assertTrue(launcherService.lastOpenedUrl().endsWith(".ics"));
        assertTrue(finished.taskPlan().steps().stream().allMatch(step -> "done".equals(step.status())));
    }

    @Test
    void assistantService_restoresActivePlanFromPreferences() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService firstService = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);
        AssistantResponse planned = firstService.handleTypedInput("create a reminder, draft an email, and open the calendar for tomorrow");

        assertNotNull(planned.taskPlan());

        AssistantService restoredService = new AssistantService(llmClient, promptBuilder, speechService, new IntentRouter(), fallbackChatService, null, 12);
        AssistantResponse restored = restoredService.handleTypedInput("show plan status");

        assertNotNull(restored.taskPlan());
        assertEquals(planned.taskPlan().goal(), restored.taskPlan().goal());
        assertEquals(planned.taskPlan().steps().size(), restored.taskPlan().steps().size());
    }

    @Test
    void clearConversationState_resetsPlanAndPendingApprovalState() {
        LlmClient llmClient = prompt -> "unused";
        PromptBuilder promptBuilder = new TestPromptBuilder();
        SpeechService speechService = new SpeechService(new SpeechRecognizer(null, new TypedFallbackService(), null));
        TrackingFallbackChatService fallbackChatService = new TrackingFallbackChatService(llmClient, promptBuilder, 12);
        IntentRouter intentRouter = new IntentRouter();

        AssistantService service = new AssistantService(llmClient, promptBuilder, speechService, intentRouter, fallbackChatService, null, 12);

        AssistantResponse planned = service.handleTypedInput("create a reminder, draft an email, and open the calendar for tomorrow");
        AssistantResponse pending = service.handleTypedInput("file move notes.txt to archive/notes.txt");

        assertNotNull(planned.taskPlan());
        assertTrue(pending.requiresApproval());

        service.clearConversationState();

        AssistantResponse status = service.handleTypedInput("show plan status");
        AssistantResponse approval = service.handleTypedInput("approve last action");

        assertEquals(IntentType.GENERAL_CHAT, status.intentType());
        assertFalse(status.answer().contains("current plan status"));
        assertEquals(IntentType.GENERAL_CHAT, approval.intentType());
        assertNull(PreferencesService.instance().getPendingApproval());
        assertNull(PreferencesService.instance().getActivePlan());
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
