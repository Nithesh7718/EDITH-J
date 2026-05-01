package com.edithj.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.edithj.memory.InMemoryMemoryRepository;
import com.edithj.memory.MemoryEntry;
import com.edithj.memory.MemoryService;

class ConversationHistoryServiceTest {

    @Test
    void archiveToMemoryAndClear_movesChatHistoryIntoMemory() {
        InMemoryChatRepository chatRepository = new InMemoryChatRepository();
        ConversationHistoryService historyService = new ConversationHistoryService(chatRepository);
        InMemoryMemoryRepository memoryRepository = new InMemoryMemoryRepository();
        MemoryService memoryService = new MemoryService(memoryRepository);

        historyService.appendUserMessage("remind me tomorrow");
        historyService.appendMessage("edith", "I can help with that.");

        int archived = historyService.archiveToMemoryAndClear(memoryService, 50);

        assertEquals(2, archived);
        assertTrue(historyService.getRecentHistory(10).isEmpty());

        List<MemoryEntry> entries = memoryService.recent(10);
        assertEquals(1, entries.size());
        assertEquals("chat_archive", entries.get(0).category());
        assertTrue(entries.get(0).content().contains("remind me tomorrow"));
        assertTrue(entries.get(0).content().contains("I can help with that."));
    }

    private static final class InMemoryChatRepository implements ChatRepository {

        private final List<ChatMessage> messages = new ArrayList<>();

        @Override
        public List<ChatMessage> findRecent(int limit) {
            int safeLimit = Math.max(0, limit);
            if (safeLimit == 0 || messages.isEmpty()) {
                return List.of();
            }
            int fromIndex = Math.max(0, messages.size() - safeLimit);
            return List.copyOf(messages.subList(fromIndex, messages.size()));
        }

        @Override
        public void save(ChatMessage message) {
            messages.add(message);
        }

        @Override
        public void clear() {
            messages.clear();
        }
    }
}
