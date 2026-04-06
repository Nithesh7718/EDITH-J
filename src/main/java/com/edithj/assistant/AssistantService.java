package com.edithj.assistant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

import com.edithj.integration.llm.GroqClient;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.speech.SpeechService;

public class AssistantService {

    private static final int DEFAULT_MEMORY_WINDOW = 12;

    private record ChatTurn(String role, String text) {

    }

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final SpeechService speechService;
    private final Deque<ChatTurn> memoryWindow;
    private final int maxTurns;

    private final String systemPrompt;
    private String lastVoiceTranscript = "";

    public AssistantService() {
        this(new GroqClient(), new PromptBuilder(), new SpeechService(), DEFAULT_MEMORY_WINDOW);
    }

    public AssistantService(LlmClient llmClient,
            PromptBuilder promptBuilder,
            SpeechService speechService,
            int maxTurns) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.speechService = speechService;
        this.maxTurns = Math.max(2, maxTurns);
        this.memoryWindow = new ArrayDeque<>(this.maxTurns);
        this.systemPrompt = promptBuilder.loadSystemPrompt();
    }

    public void configureTypedFallbackProvider(Function<String, String> typedInputProvider) {
        speechService.typedFallbackService().setTypedInputProvider(typedInputProvider);
    }

    public String respondToTypedInput(String text) {
        return processMessage(text, "typed");
    }

    public void startVoiceInput() {
        speechService.startListening();
    }

    public String stopVoiceInputAndRespond() {
        SpeechService.CapturedInput capturedInput = speechService.stopListening();
        lastVoiceTranscript = capturedInput.text();

        if (capturedInput.text().isBlank()) {
            return "I did not catch that. Please type your request.";
        }

        String channel = capturedInput.usedTypedFallback() ? "voice-typed-fallback" : "voice";
        return processMessage(capturedInput.text(), channel);
    }

    public String getLastVoiceTranscript() {
        return lastVoiceTranscript;
    }

    private String processMessage(String userText, String channel) {
        String normalized = normalizeUserText(userText);
        if (normalized.isBlank()) {
            return "Please enter a message.";
        }

        remember("user", normalized);

        String prompt = buildPromptWithMemory(normalized, channel);
        String reply = llmClient.generateReply(prompt);
        String normalizedReply = (reply == null || reply.isBlank())
                ? "I could not generate a response right now."
                : reply.trim();

        remember("assistant", normalizedReply);
        return normalizedReply;
    }

    private String buildPromptWithMemory(String latestUserInput, String channel) {
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

    private String normalizeUserText(String userText) {
        return userText == null ? "" : userText.trim();
    }
}
