package com.edithj.ai;

import java.util.Objects;

import com.edithj.integration.llm.GroqClient;
import com.edithj.integration.llm.LlmClient;

public class GroqChatService implements ChatService {

    private final LlmClient llmClient;

    public GroqChatService() {
        this(new GroqClient());
    }

    public GroqChatService(LlmClient llmClient) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
    }

    @Override
    public String generateReply(String prompt) {
        return llmClient.generateReply(prompt);
    }
}
