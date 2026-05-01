package com.edithj.chat;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.edithj.assistant.AssistantResponse;
import com.edithj.memory.MemoryService;

public class ConversationHistoryService {

    private static final int MEMORY_ARCHIVE_CHAR_LIMIT = 4000;
    private static final DateTimeFormatter MEMORY_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

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
                response.success(),
                response.requiresApproval(),
                response.approvalType(),
                response.explanation(),
                response.taskPlan() == null ? "" : response.taskPlan().goal(),
                response.taskPlan(),
                response.actions(),
                response.recoveryOptions(),
                response.metadata()));
    }

    public void clearHistory() {
        repository.clear();
    }

    public int archiveToMemoryAndClear(MemoryService memoryService, int limit) {
        List<ChatMessage> history = getRecentHistory(limit);
        if (history.isEmpty()) {
            clearHistory();
            return 0;
        }
        if (memoryService != null) {
            memoryService.add("chat_archive", formatArchive(history));
        }
        clearHistory();
        return history.size();
    }

    private String formatArchive(List<ChatMessage> history) {
        ChatMessage first = history.get(0);
        ChatMessage last = history.get(history.size() - 1);
        StringBuilder archive = new StringBuilder();
        archive.append("Archived chat history from ")
                .append(MEMORY_TIME_FORMAT.format(first.timestamp()))
                .append(" to ")
                .append(MEMORY_TIME_FORMAT.format(last.timestamp()))
                .append('\n');
        for (ChatMessage message : history) {
            if (archive.length() >= MEMORY_ARCHIVE_CHAR_LIMIT) {
                break;
            }
            archive.append(message.role())
                    .append(" [")
                    .append(MEMORY_TIME_FORMAT.format(message.timestamp()));
            if (!message.source().isBlank()) {
                archive.append(" | ").append(message.source());
            }
            archive.append("]: ")
                    .append(message.content())
                    .append('\n');
        }
        if (archive.length() > MEMORY_ARCHIVE_CHAR_LIMIT) {
            return archive.substring(0, MEMORY_ARCHIVE_CHAR_LIMIT - 3) + "...";
        }
        return archive.toString().trim();
    }
}
