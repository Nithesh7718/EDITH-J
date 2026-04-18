package com.edithj.memory;

import java.util.List;

public interface MemoryRepository {

    MemoryEntry save(MemoryEntry entry);

    List<MemoryEntry> findRecent(int limit);

    List<MemoryEntry> search(String query, int limit);
}
