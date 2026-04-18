package com.edithj.memory;

import java.util.ArrayList;
import java.util.List;

public class InMemoryMemoryRepository implements MemoryRepository {

    private final List<MemoryEntry> entries = new ArrayList<>();

    @Override
    public synchronized MemoryEntry save(MemoryEntry entry) {
        entries.add(entry);
        return entry;
    }

    @Override
    public synchronized List<MemoryEntry> findRecent(int limit) {
        if (limit <= 0 || entries.isEmpty()) {
            return List.of();
        }

        int from = Math.max(0, entries.size() - limit);
        List<MemoryEntry> copy = new ArrayList<>(entries.subList(from, entries.size()));
        java.util.Collections.reverse(copy);
        return copy;
    }

    @Override
    public synchronized List<MemoryEntry> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return findRecent(limit);
        }

        String normalized = query.trim().toLowerCase();
        List<MemoryEntry> filtered = entries.stream()
                .filter(entry -> entry.category().toLowerCase().contains(normalized)
                || entry.content().toLowerCase().contains(normalized))
                .toList();

        int size = filtered.size();
        int safeLimit = Math.max(1, limit);
        int from = Math.max(0, size - safeLimit);

        List<MemoryEntry> copy = new ArrayList<>(filtered.subList(from, size));
        java.util.Collections.reverse(copy);
        return copy;
    }
}
