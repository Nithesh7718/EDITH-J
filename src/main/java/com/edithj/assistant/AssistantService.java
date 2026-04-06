package com.edithj.assistant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.edithj.integration.llm.GroqClient;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.speech.SpeechService;

public class AssistantService {

    @FunctionalInterface
    public interface NotesOrchestrator {

        String handle(String normalizedInput, String payload);
    }

    @FunctionalInterface
    public interface RemindersOrchestrator {

        String handle(String normalizedInput, String payload);
    }

    @FunctionalInterface
    public interface AppLaunchOrchestrator {

        String handle(String normalizedInput, String payload);
    }

    private static final int DEFAULT_MEMORY_WINDOW = 12;

    private record ChatTurn(String role, String text) {

    }

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final SpeechService speechService;
    private final IntentRouter intentRouter;
    private final Deque<ChatTurn> memoryWindow;
    private final int maxTurns;

    private final String systemPrompt;

    private NotesOrchestrator notesOrchestrator;
    private RemindersOrchestrator remindersOrchestrator;
    private AppLaunchOrchestrator appLaunchOrchestrator;

    private String lastVoiceTranscript = "";

    public AssistantService() {
        this(new GroqClient(), new PromptBuilder(), new SpeechService(), new IntentRouter(), DEFAULT_MEMORY_WINDOW);
    }

    public AssistantService(LlmClient llmClient,
            PromptBuilder promptBuilder,
            SpeechService speechService,
            IntentRouter intentRouter,
            int maxTurns) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.speechService = Objects.requireNonNull(speechService, "speechService");
        this.intentRouter = Objects.requireNonNull(intentRouter, "intentRouter");
        this.maxTurns = Math.max(2, maxTurns);
        this.memoryWindow = new ArrayDeque<>(this.maxTurns);
        this.systemPrompt = this.promptBuilder.loadSystemPrompt();

        this.notesOrchestrator = (input, payload) -> "Notes service is not wired yet.";
        this.remindersOrchestrator = (input, payload) -> "Reminders service is not wired yet.";
        this.appLaunchOrchestrator = (input, payload) -> "App launcher service is not wired yet.";
    }

    public void setNotesOrchestrator(NotesOrchestrator notesOrchestrator) {
        this.notesOrchestrator = Objects.requireNonNull(notesOrchestrator, "notesOrchestrator");
    }

    public void setRemindersOrchestrator(RemindersOrchestrator remindersOrchestrator) {
        this.remindersOrchestrator = Objects.requireNonNull(remindersOrchestrator, "remindersOrchestrator");
    }

    public void setAppLaunchOrchestrator(AppLaunchOrchestrator appLaunchOrchestrator) {
        this.appLaunchOrchestrator = Objects.requireNonNull(appLaunchOrchestrator, "appLaunchOrchestrator");
    }

    public void configureTypedFallbackProvider(Function<String, String> typedInputProvider) {
        speechService.typedFallbackService().setTypedInputProvider(typedInputProvider);
    }

    public AssistantResponse handleTypedInput(String inputText) {
        return handleIncomingInput(inputText, "typed");
    }

    public String respondToTypedInput(String inputText) {
        return handleTypedInput(inputText).answer();
    }

    public void startVoiceInput() {
        speechService.startListening();
    }

    public AssistantResponse stopVoiceInputAndHandle() {
        SpeechService.CapturedInput capturedInput = speechService.stopListening();
        lastVoiceTranscript = capturedInput.text();

        String channel = capturedInput.usedTypedFallback() ? "voice-typed-fallback" : "voice";
        return handleIncomingInput(capturedInput.text(), channel);
    }

    public String stopVoiceInputAndRespond() {
        return stopVoiceInputAndHandle().answer();
    }

    public String getLastVoiceTranscript() {
        return lastVoiceTranscript;
    }

    private AssistantResponse handleIncomingInput(String rawInput, String channel) {
        String normalized = normalize(rawInput);
        if (normalized.isBlank()) {
            return new AssistantResponse(IntentType.FALLBACK_CHAT, "", "Please enter a message.", channel);
        }

        remember("user", normalized);

        IntentRouter.RoutedIntent routedIntent = intentRouter.route(normalized);
        String answer = routeAndExecute(routedIntent, channel);
        remember("assistant", answer);

        return new AssistantResponse(routedIntent.intentType(), normalized, answer, channel);
    }

    private String routeAndExecute(IntentRouter.RoutedIntent routedIntent, String channel) {
        String answer;
        switch (routedIntent.intentType()) {
            case NOTES ->
                answer = notesOrchestrator.handle(routedIntent.normalizedInput(), routedIntent.payload());
            case REMINDERS ->
                answer = remindersOrchestrator.handle(routedIntent.normalizedInput(), routedIntent.payload());
            case APP_LAUNCH ->
                answer = appLaunchOrchestrator.handle(routedIntent.normalizedInput(), routedIntent.payload());
            case FALLBACK_CHAT ->
                answer = fallbackChat(routedIntent.normalizedInput(), channel);
            default ->
                answer = fallbackChat(routedIntent.normalizedInput(), channel);
        }

        if (answer == null || answer.isBlank()) {
            return "I could not complete that request.";
        }
        return answer.trim();
    }

    private String fallbackChat(String normalizedInput, String channel) {
        String prompt = buildPromptWithMemory(channel);
        String reply = llmClient.generateReply(prompt);
        if (reply == null || reply.isBlank()) {
            return "I could not generate a response right now.";
        }
        return reply.trim();
    }

    private String buildPromptWithMemory(String channel) {
        StringBuilder prompt = new StringBuilder();

        if (!systemPrompt.isBlank()) {
            prompt.append(systemPrompt.trim()).append("\n\n");
        }

        prompt.append("Input channel: ").append(channel).append("\n");
        prompt.append("Conversation memory:\n");

        for (ChatTurn turn : snapshotMemory()) {
            prompt.append(turn.role()).append(": ").append(turn.text()).append("\n");
        }

        prompt.append("assistant: ");
        return prompt.toString();
    }

    private synchronized void remember(String role, String text) {
        if (memoryWindow.size() >= maxTurns) {
            memoryWindow.removeFirst();
        }
        memoryWindow.addLast(new ChatTurn(role, text));
    }

    private synchronized List<ChatTurn> snapshotMemory() {
        return new ArrayList<>(memoryWindow);
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim();
    }
}
