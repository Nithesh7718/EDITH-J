package com.edithj.chat;

import java.util.List;

import com.edithj.assistant.AssistantResponse;

public class ConversationHistoryService {

    private final ChatRepository repository;

    public ConversationHistoryService(ChatRepository repository) {
        this.repository = repository;
    }

    public List<ChatMessage> getRecentHistory(int limit) {
        return repository.findRecent(limit);
    }

    public void appendMessage(String role, String content) {
        repository.save(new ChatMessage(role, content));
    }

    public void appendUserMessage(String content) {
        repository.save(new ChatMessage("user", content, "User", "", true));
    }

    public void appendAssistantResponse(AssistantResponse response) {
        if (response == null) {
            return;
        }
        repository.save(new ChatMessage(
                "edith",
                response.answer(),
                response.source(),
                response.intentType().name(),
                response.success()));
    }

    public void clearHistory() {
        repository.clear();
    }
}
