package com.edithj.reminders;

import java.time.Instant;
import java.util.UUID;

public class Reminder {

    private String id;
    private String text;
    private Instant dueAt;
    private boolean isCompleted;
    private Instant createdAt;
    private Instant updatedAt;

    public Reminder() {
        // Required for JSON deserialization.
    }

    public Reminder(String id, String text, Instant dueAt, boolean isCompleted, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.text = text;
        this.dueAt = dueAt;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Reminder newReminder(String text, Instant dueAt) {
        String normalizedText = text == null ? "" : text.trim();
        Instant now = Instant.now();
        return new Reminder(UUID.randomUUID().toString(), normalizedText, dueAt, false, now, now);
    }

    public void markDone() {
        this.isCompleted = true;
        this.updatedAt = Instant.now();
    }

    public void update(String text, Instant dueAt) {
        this.text = text == null ? "" : text.trim();
        this.dueAt = dueAt;
        this.updatedAt = Instant.now();
    }

    public String summary() {
        if (text == null || text.isBlank()) {
            return "Untitled reminder";
        }
        return text.length() <= 48 ? text : text.substring(0, 48) + "...";
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.toLowerCase();
        String inText = text == null ? "" : text.toLowerCase();
        return inText.contains(normalized);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
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
    public String toString() {
        return "Reminder {"
                + "id='" + id + '\''
                + ", text='" + text + '\''
                + ", dueAt=" + dueAt
                + ", isCompleted=" + isCompleted
                + '}';
    }
}
