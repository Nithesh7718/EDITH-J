package com.edithj.chat;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(String id, String role, String content, Instant timestamp, String source, String intentType, boolean success) {
    public ChatMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        role = role == null || role.isBlank() ? "edith" : role;
        content = content == null ? "" : content;
        timestamp = timestamp == null ? Instant.now() : timestamp;
        source = source == null || source.isBlank() ? defaultSource(role) : source;
        intentType = intentType == null ? "" : intentType;
    }

    public ChatMessage(String role, String content) {
        this(UUID.randomUUID().toString(), role, content, Instant.now(), defaultSource(role), "", true);
    }

    public ChatMessage(String role, String content, String source, String intentType, boolean success) {
        this(UUID.randomUUID().toString(), role, content, Instant.now(), source, intentType, success);
    }

    private static String defaultSource(String role) {
        return "user".equalsIgnoreCase(role) ? "User" : "AI";
    }
}
