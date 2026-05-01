package com.edithj.chat;

import java.util.List;

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

    public void clearHistory() {
        repository.clear();
    }
}
