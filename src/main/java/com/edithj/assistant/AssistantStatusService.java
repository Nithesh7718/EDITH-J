package com.edithj.assistant;

public final class AssistantStatusService {

    private static final AssistantStatusService INSTANCE = new AssistantStatusService();

    private volatile AssistantStatus currentStatus = AssistantStatus.OFFLINE;
    private volatile String statusMessage = "Groq unreachable";

    private AssistantStatusService() {
    }

    public static AssistantStatusService instance() {
        return INSTANCE;
    }

    public AssistantStatus status() {
        return currentStatus;
    }

    public String message() {
        return statusMessage;
    }

    public synchronized void markOnline(String message) {
        this.currentStatus = AssistantStatus.ONLINE;
        this.statusMessage = normalizeMessage(message, "AI ready");
    }

    public synchronized void markOffline(String message) {
        this.currentStatus = AssistantStatus.OFFLINE;
        this.statusMessage = normalizeMessage(message, "Groq unreachable");
    }

    private String normalizeMessage(String input, String fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        return input.trim();
    }
}
