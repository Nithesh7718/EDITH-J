package com.edithj.memory;

import java.util.ArrayList;
import java.util.List;

public class MemoryService {

    private final List<MemoryEntry> entries = new ArrayList<>();

    public synchronized MemoryEntry add(String category, String content) {
        MemoryEntry entry = new MemoryEntry(category, content);
        entries.add(entry);
        return entry;
    }

    public synchronized List<MemoryEntry> recent(int limit) {
        int size = entries.size();
        if (size == 0 || limit <= 0) {
            return List.of();
        }

        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(entries.subList(fromIndex, size));
    }

    public synchronized List<MemoryEntry> search(String query) {
        if (query == null || query.isBlank()) {
            return recent(20);
        }

        String normalizedQuery = query.trim().toLowerCase();
        return entries.stream()
                .filter(entry -> entry.content().toLowerCase().contains(normalizedQuery)
                || entry.category().toLowerCase().contains(normalizedQuery))
                .toList();
    }
}
