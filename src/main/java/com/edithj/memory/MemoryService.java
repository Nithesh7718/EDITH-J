package com.edithj.memory;

import java.util.List;

import com.edithj.storage.RepositoryFactory;

public class MemoryService {

    private final MemoryRepository memoryRepository;

    public MemoryService() {
        this(RepositoryFactory.createMemoryRepository());
    }

    public MemoryService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    public MemoryEntry add(String category, String content) {
        MemoryEntry entry = new MemoryEntry(category, content);
        return memoryRepository.save(entry);
    }

    public List<MemoryEntry> recent(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return memoryRepository.findRecent(limit);
    }

    public List<MemoryEntry> search(String query) {
        return memoryRepository.search(query, 20);
    }
}
