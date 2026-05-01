package com.edithj.assistant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.ai.PromptTemplateService;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.memory.MemoryEntry;
import com.edithj.memory.MemoryService;

public class FallbackChatService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackChatService.class);
        private static final Pattern PROVIDER_OVERRIDE_PATTERN = Pattern.compile(
            "(?i)^\\s*use\\s+(groq|gemini|openai|sarvam)\\s+for\\s+this\\s+answer[,:]?\\s*(.*)$");

    private record ChatTurn(String role, String text) {

    }

    private static final String DEFAULT_REPLY = "I could not generate a response right now.";
    private static final int PROMPT_MEMORY_LIMIT = 6;

    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplateService;
    private final MemoryService memoryService;
    private final AssistantStatusService assistantStatusService;
    private final Deque<ChatTurn> memoryWindow;
    private final int maxTurns;

    public FallbackChatService(LlmClient llmClient, PromptBuilder promptBuilder, int maxTurns) {
        this(
                llmClient,
                new PromptTemplateService(com.edithj.config.AppConfig.load(), promptBuilder),
                createMemoryServiceSafely(),
                AssistantStatusService.instance(),
                maxTurns);
    }

    FallbackChatService(LlmClient llmClient,
            PromptTemplateService promptTemplateService,
            MemoryService memoryService,
            AssistantStatusService assistantStatusService,
            int maxTurns) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
        this.promptTemplateService = Objects.requireNonNull(promptTemplateService, "promptTemplateService");
        this.memoryService = memoryService;
        this.assistantStatusService = Objects.requireNonNull(assistantStatusService, "assistantStatusService");
        this.maxTurns = Math.max(2, maxTurns);
        this.memoryWindow = new ArrayDeque<>(this.maxTurns);
    }

    public synchronized void recordUserTurn(String text) {
        remember("user", text);
    }

    public synchronized void recordAssistantTurn(String text) {
        remember("assistant", text);
    }

    public synchronized void populateMemory(List<com.edithj.chat.ChatMessage> history) {
        memoryWindow.clear();
        for (com.edithj.chat.ChatMessage message : history) {
            remember(message.role(), message.content());
        }
    }

    public synchronized void clearConversationMemory() {
        memoryWindow.clear();
    }

    String buildPromptWithMemory(String channel) {
        StringBuilder prompt = new StringBuilder();
        String systemPrompt = resolveSystemPrompt();

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
        String providerOverride = detectProviderOverride();
        if (!providerOverride.isBlank()) {
            prompt = "[[provider:" + providerOverride + "]]\n" + prompt;
        }
        String reply = llmClient.generateReply(prompt);
        if (reply == null || reply.isBlank()) {
            assistantStatusService.markOffline("AI provider unreachable");
            return DEFAULT_REPLY;
        }

        String trimmed = reply.trim();
        if (indicatesProviderConnectivityFailure(trimmed)) {
            assistantStatusService.markOffline("AI provider unreachable");
        } else {
            assistantStatusService.markOnline("AI ready");
        }
        return trimmed;
    }

    private String detectProviderOverride() {
        List<ChatTurn> turns = snapshotMemory();
        for (int index = turns.size() - 1; index >= 0; index--) {
            ChatTurn turn = turns.get(index);
            if (!"user".equalsIgnoreCase(turn.role())) {
                continue;
            }
            Matcher matcher = PROVIDER_OVERRIDE_PATTERN.matcher(turn.text());
            if (matcher.matches()) {
                return matcher.group(1).toLowerCase(Locale.ROOT);
            }
            break;
        }
        return "";
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

    private String resolveSystemPrompt() {
        String basePrompt = safePrompt(promptTemplateService.systemPrompt());
        if (memoryService == null) {
            return basePrompt;
        }

        try {
            List<MemoryEntry> entries = memoryService.recent(PROMPT_MEMORY_LIMIT);
            if (entries == null || entries.isEmpty()) {
                return basePrompt;
            }

            String enrichedPrompt = safePrompt(promptTemplateService.systemPromptWithMemory(entries));
            return enrichedPrompt.isBlank() ? basePrompt : enrichedPrompt;
        } catch (RuntimeException exception) {
            logger.debug("Unable to enrich system prompt with memory; using base prompt", exception);
            return basePrompt;
        }
    }

    private String safePrompt(String prompt) {
        return prompt == null ? "" : prompt;
    }

    private static MemoryService createMemoryServiceSafely() {
        try {
            return new MemoryService();
        } catch (RuntimeException exception) {
            logger.debug("MemoryService unavailable for fallback prompt enrichment", exception);
            return null;
        }
    }

    private boolean indicatesProviderConnectivityFailure(String reply) {
        if (reply == null || reply.isBlank()) {
            return true;
        }

        String lower = reply.toLowerCase(Locale.ROOT);
        return lower.contains("httpconnecttimeoutexception")
                || lower.contains("connect timed out")
                || lower.contains("unable to reach")
                || lower.contains("request failed with http")
                || lower.contains("request was interrupted");
    }
}
