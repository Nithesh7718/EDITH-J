package com.edithj.chat;

import java.util.List;

public interface ChatRepository {
    List<ChatMessage> findRecent(int limit);
    void save(ChatMessage message);
    void clear();
}
