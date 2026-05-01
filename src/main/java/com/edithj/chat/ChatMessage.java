package com.edithj.chat;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(String id, String role, String content, Instant timestamp) {
    public ChatMessage(String role, String content) {
        this(UUID.randomUUID().toString(), role, content, Instant.now());
    }
}
