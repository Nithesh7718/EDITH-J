package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;

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
}
