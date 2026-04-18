package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.edithj.ai.PromptTemplateService;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.memory.MemoryEntry;
import com.edithj.memory.MemoryService;

class FallbackChatServiceTest {

    @Test
    void buildPromptWithMemory_includesSystemPromptChannelAndTurns() {
        LlmClient llmClient = prompt -> "ok";
        PromptBuilder promptBuilder = new TestPromptBuilder();

        FallbackChatService service = new FallbackChatService(llmClient, promptBuilder, 4);
        service.recordUserTurn("hello");
        service.recordAssistantTurn("hi there");
        service.recordUserTurn("what can you do");

        String prompt = service.buildPromptWithMemory("typed");

        assertTrue(prompt.startsWith("System prompt line\n\nInput channel: typed\nConversation memory:\n"));
        assertTrue(prompt.contains("user: hello\n"));
        assertTrue(prompt.contains("assistant: hi there\n"));
        assertTrue(prompt.contains("user: what can you do\n"));
        assertTrue(prompt.endsWith("assistant: "));
    }

    private static final class TestPromptBuilder extends PromptBuilder {

        @Override
        public String loadSystemPrompt() {
            return "System prompt line";
        }
    }

    @Test
    void buildPromptWithMemory_enrichesSystemPromptWithRecentMemoryEntries() {
        LlmClient llmClient = prompt -> "ok";
        CapturingPromptTemplateService promptTemplateService = new CapturingPromptTemplateService();
        MemoryService memoryService = new TestMemoryService(List.of(
                new MemoryEntry("prefs", "Use concise responses"),
                new MemoryEntry("tasks", "Follow up on migration status")));

        FallbackChatService service = new FallbackChatService(
                llmClient,
                promptTemplateService,
                memoryService,
                AssistantStatusService.instance(),
                4);
        service.recordUserTurn("hello");

        String prompt = service.buildPromptWithMemory("typed");

        assertEquals(2, promptTemplateService.capturedEntries().size());
        assertTrue(prompt.startsWith("System prompt line with memory\n\n"));
        assertTrue(prompt.contains("Input channel: typed"));
    }

    @Test
    void runFallbackChat_marksAssistantOfflineOnConnectivityFailureResponse() {
        AssistantStatusService statusService = AssistantStatusService.instance();
        AssistantStatus previousStatus = statusService.status();
        String previousMessage = statusService.message();

        try {
            statusService.markOnline("AI ready");
            LlmClient llmClient = prompt -> "java.net.http.HttpConnectTimeoutException: HTTP connect timed out";
            FallbackChatService service = new FallbackChatService(llmClient, new TestPromptBuilder(), 4);

            service.runFallbackChat("typed");

            assertEquals(AssistantStatus.OFFLINE, statusService.status());
            assertEquals("Groq unreachable", statusService.message());
        } finally {
            if (previousStatus == AssistantStatus.ONLINE) {
                statusService.markOnline(previousMessage);
            } else {
                statusService.markOffline(previousMessage);
            }
        }
    }

    private static final class TestMemoryService extends MemoryService {

        private final List<MemoryEntry> entries;

        TestMemoryService(List<MemoryEntry> entries) {
            this.entries = entries;
        }

        @Override
        public List<MemoryEntry> recent(int limit) {
            return entries;
        }
    }

    private static final class CapturingPromptTemplateService extends PromptTemplateService {

        private List<MemoryEntry> capturedEntries = List.of();

        @Override
        public String systemPrompt() {
            return "System prompt line";
        }

        @Override
        public String systemPromptWithMemory(List<MemoryEntry> memoryEntries) {
            this.capturedEntries = memoryEntries;
            return "System prompt line with memory";
        }

        List<MemoryEntry> capturedEntries() {
            return capturedEntries;
        }
    }
}
