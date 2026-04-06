package com.edithj.notes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Note {

    private String id;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public Note() {
        // Required for JSON deserialization.
    }

    public Note(String id, String title, String content, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Note newNote(String title, String content) {
        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedContent = content == null ? "" : content.trim();
        Instant now = Instant.now();
        return new Note(UUID.randomUUID().toString(), normalizedTitle, normalizedContent, now, now);
    }

    public void update(String title, String content) {
        this.title = title == null ? "" : title.trim();
        this.content = content == null ? "" : content.trim();
        this.updatedAt = Instant.now();
    }

    public String summary() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        if (content == null || content.isBlank()) {
            return "Untitled note";
        }
        return content.length() <= 48 ? content : content.substring(0, 48) + "...";
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.toLowerCase();
        String inTitle = title == null ? "" : title.toLowerCase();
        String inContent = content == null ? "" : content.toLowerCase();
        return inTitle.contains(normalized) || inContent.contains(normalized);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Note note)) {
            return false;
        }
        return Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
