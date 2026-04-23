package com.edithj.assistant;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.commands.CalendarCommandHandler;
import com.edithj.commands.CommandHandler;
import com.edithj.commands.DesktopToolsCommandHandler;
import com.edithj.commands.EmailCommandHandler;
import com.edithj.commands.FallbackChatHandler;
import com.edithj.commands.LauncherCommandHandler;
import com.edithj.commands.NotesCommandHandler;
import com.edithj.commands.ReminderCommandHandler;
import com.edithj.commands.UtilitiesCommandHandler;
import com.edithj.commands.WeatherCommandHandler;
import com.edithj.commands.WhatsAppCommandHandler;
import com.edithj.integration.llm.GroqClient;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.speech.SpeechService;

public class AssistantService {

    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);
    private static final int DEFAULT_MEMORY_WINDOW = 12;
    private static final Pattern WHATSAPP_FOLLOWUP_SEND_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:(?:send|sen|snd|message|msg|text)\\b.*|(?:just|jusdt)\\s+send\\b.*)");

    private final SpeechService speechService;
    private final IntentRouter intentRouter;
    private final FallbackChatService fallbackChatService;
    private String lastVoiceTranscript = "";
    private IntentType lastStructuredIntent = IntentType.FALLBACK_CHAT;

    public AssistantService() {
        this(new GroqClient(), new PromptBuilder(), new SpeechService(), new IntentRouter(), DEFAULT_MEMORY_WINDOW);
    }

    public AssistantService(LlmClient llmClient,
            PromptBuilder promptBuilder,
            SpeechService speechService,
            IntentRouter intentRouter,
            int maxTurns) {
        this(llmClient, promptBuilder, speechService, intentRouter,
                new FallbackChatService(llmClient, promptBuilder, maxTurns), maxTurns);
    }

    AssistantService(LlmClient llmClient,
            PromptBuilder promptBuilder,
            SpeechService speechService,
            IntentRouter intentRouter,
            FallbackChatService fallbackChatService,
            int maxTurns) {
        Objects.requireNonNull(llmClient, "llmClient");
        Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.speechService = Objects.requireNonNull(speechService, "speechService");
        this.intentRouter = Objects.requireNonNull(intentRouter, "intentRouter");
        this.fallbackChatService = Objects.requireNonNull(fallbackChatService, "fallbackChatService");

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
        fallbackChatService.recordUserTurn(normalized);
        AssistantResponse response = routeWithContextRecovery(normalized, channel);
        logger.info("Intent routed to: {} | Response length: {}", response.intentType(), response.answer().length());
        fallbackChatService.recordAssistantTurn(response.answer());

        if (response.intentType() != IntentType.FALLBACK_CHAT) {
            lastStructuredIntent = response.intentType();
        }

        return response;
    }

    private AssistantResponse routeWithContextRecovery(String normalizedInput, String channel) {
        IntentRouter.RoutedIntent routedIntent = intentRouter.route(normalizedInput);
        if (routedIntent.intentType() != IntentType.FALLBACK_CHAT) {
            return intentRouter.routeAndHandle(routedIntent, channel);
        }

        if (shouldRecoverToWhatsApp(normalizedInput)) {
            String recoveredInput = ensureWhatsAppPrefix(normalizedInput);
            IntentRouter.RoutedIntent recoveredIntent = intentRouter.route(recoveredInput);
            if (recoveredIntent.intentType() == IntentType.WHATSAPP) {
                return intentRouter.routeAndHandle(recoveredIntent, channel);
            }
        }

        return intentRouter.routeAndHandle(routedIntent, channel);
    }

    private boolean shouldRecoverToWhatsApp(String input) {
        if (lastStructuredIntent != IntentType.WHATSAPP) {
            return false;
        }
        String normalized = normalize(input).toLowerCase();
        if (normalized.isBlank()) {
            return false;
        }
        return WHATSAPP_FOLLOWUP_SEND_PATTERN.matcher(normalized).matches();
    }

    private String ensureWhatsAppPrefix(String input) {
        String normalized = normalize(input);
        if (normalized.toLowerCase().contains("whatsapp")
                || normalized.toLowerCase().contains("whtsapp")
                || normalized.toLowerCase().contains("whatsap")
                || normalized.toLowerCase().contains("watsapp")) {
            return normalized;
        }
        return "whatsapp " + normalized;
    }

    private void registerDefaultHandlers() {
        intentRouter.registerHandler(new NotesCommandHandler());
        intentRouter.registerHandler(new ReminderCommandHandler());
        intentRouter.registerHandler(new LauncherCommandHandler());
        intentRouter.registerHandler(new EmailCommandHandler());
        intentRouter.registerHandler(new CalendarCommandHandler());
        intentRouter.registerHandler(new WhatsAppCommandHandler());
        intentRouter.registerHandler(new WeatherCommandHandler());
        intentRouter.registerHandler(new UtilitiesCommandHandler());
        intentRouter.registerHandler(new DesktopToolsCommandHandler());
        intentRouter.registerHandler(new FallbackChatHandler(context -> fallbackChatService.runFallbackChat(context.channel())));
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim();
    }
}
