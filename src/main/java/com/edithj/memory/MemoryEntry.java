package com.edithj.memory;

import java.time.Instant;
import java.util.UUID;

public class MemoryEntry {

    private final String id;
    private final String category;
    private final String content;
    private final Instant createdAt;

    public MemoryEntry(String category, String content) {
        this.id = UUID.randomUUID().toString();
        this.category = category == null ? "general" : category.trim();
        this.content = content == null ? "" : content.trim();
        this.createdAt = Instant.now();
    }

    public String id() {
        return id;
    }

    public String category() {
        return category;
    }

    public String content() {
        return content;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
