package com.edithj.assistant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.commands.CommandHandler;
import com.edithj.commands.DesktopToolsCommandHandler;
import com.edithj.commands.FallbackChatHandler;
import com.edithj.commands.LauncherCommandHandler;
import com.edithj.commands.NotesCommandHandler;
import com.edithj.commands.ReminderCommandHandler;
import com.edithj.commands.UtilitiesCommandHandler;
import com.edithj.commands.WeatherCommandHandler;
import com.edithj.integration.llm.GroqClient;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.speech.SpeechService;

public class AssistantService {

    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);
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

        registerDefaultHandlers();
    }

    public void configureTypedFallbackProvider(Function<String, String> typedInputProvider) {
        speechService.typedFallbackService().setTypedInputProvider(typedInputProvider);
    }

    public void registerCommandHandler(CommandHandler handler) {
        intentRouter.registerHandler(handler);
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
            logger.debug("Received blank input from channel: {}", channel);
            return new AssistantResponse(IntentType.FALLBACK_CHAT, "", "Please enter a message.", channel);
        }

        logger.info("Processing input from channel: {} | Input: {}", channel, normalized.substring(0, Math.min(100, normalized.length())));
        remember("user", normalized);
        AssistantResponse response = intentRouter.routeAndHandle(normalized, channel);
        logger.info("Intent routed to: {} | Response length: {}", response.intentType(), response.answer().length());
        remember("assistant", response.answer());

        return response;
    }

    private void registerDefaultHandlers() {
        intentRouter.registerHandler(new NotesCommandHandler());
        intentRouter.registerHandler(new ReminderCommandHandler());
        intentRouter.registerHandler(new LauncherCommandHandler());
        intentRouter.registerHandler(new WeatherCommandHandler());
        intentRouter.registerHandler(new UtilitiesCommandHandler());
        intentRouter.registerHandler(new DesktopToolsCommandHandler());
        intentRouter.registerHandler(new FallbackChatHandler(this::runFallbackChat));
    }

    private String runFallbackChat(CommandHandler.CommandContext context) {
        String prompt = buildPromptWithMemory(context.channel());
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
