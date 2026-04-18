package com.edithj.memory;

import java.time.Instant;
import java.util.UUID;

public class MemoryEntry {

    private final String id;
    private final String category;
    private final String content;
    private final Instant createdAt;

    public MemoryEntry(String category, String content) {
        this(UUID.randomUUID().toString(), category, content, Instant.now());
    }

    public MemoryEntry(String id, String category, String content, Instant createdAt) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
        this.category = category == null || category.isBlank() ? "general" : category.trim();
        this.content = content == null ? "" : content.trim();
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
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
