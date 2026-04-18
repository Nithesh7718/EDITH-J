package com.edithj.assistant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;

public class FallbackChatService {

    private record ChatTurn(String role, String text) {

    }

    private static final String DEFAULT_REPLY = "I could not generate a response right now.";

    private final LlmClient llmClient;
    private final String systemPrompt;
    private final Deque<ChatTurn> memoryWindow;
    private final int maxTurns;

    public FallbackChatService(LlmClient llmClient, PromptBuilder promptBuilder, int maxTurns) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
        Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.maxTurns = Math.max(2, maxTurns);
        this.memoryWindow = new ArrayDeque<>(this.maxTurns);
        this.systemPrompt = promptBuilder.loadSystemPrompt();
    }

    public synchronized void recordUserTurn(String text) {
        remember("user", text);
    }

    public synchronized void recordAssistantTurn(String text) {
        remember("assistant", text);
    }

    String buildPromptWithMemory(String channel) {
        StringBuilder prompt = new StringBuilder();

        if (!systemPrompt.isBlank()) {
            prompt.append(systemPrompt.trim()).append("\n\n");
        }

        prompt.append("Input channel: ").append(normalizeChannel(channel)).append("\n");
        prompt.append("Conversation memory:\n");

        for (ChatTurn turn : snapshotMemory()) {
            prompt.append(turn.role()).append(": ").append(turn.text()).append("\n");
        }

        prompt.append("assistant: ");
        return prompt.toString();
    }

    public String runFallbackChat(String channel) {
        String prompt = buildPromptWithMemory(channel);
        String reply = llmClient.generateReply(prompt);
        if (reply == null || reply.isBlank()) {
            return DEFAULT_REPLY;
        }
        return reply.trim();
    }

    private synchronized void remember(String role, String text) {
        if (memoryWindow.size() >= maxTurns) {
            memoryWindow.removeFirst();
        }
        memoryWindow.addLast(new ChatTurn(role, normalizeText(text)));
    }

    private synchronized List<ChatTurn> snapshotMemory() {
        return new ArrayList<>(memoryWindow);
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "typed";
        }
        return channel.trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }
}
