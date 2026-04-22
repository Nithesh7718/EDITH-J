package com.edithj.ui.model;

/**
 * Represents the current operational state of the EDITH-J voice assistant UI.
 * This enum drives CSS class switching, audio aura animations, and the status pill.
 */
public enum AssistantUiState {

    /** No active session — gentle breathing animation on aura. */
    IDLE("IDLE", "● IDLE"),

    /** Microphone is open and capturing audio. */
    LISTENING("LISTENING", "◎ LISTENING"),

    /** Speech recognised; assistant is computing a reply. */
    PROCESSING("PROCESSING", "⟳ PROCESSING"),

    /** TTS or text reply is being delivered. */
    SPEAKING("SPEAKING", "▶ SPEAKING");

    private final String id;
    private final String displayLabel;

    AssistantUiState(String id, String displayLabel) {
        this.id = id;
        this.displayLabel = displayLabel;
    }

    /** Identifier used in CSS style-class lookups, e.g., {@code state-listening}. */
    public String cssClass() {
        return "state-" + id.toLowerCase();
    }

    /** Human-readable label shown inside the status pill. */
    public String displayLabel() {
        return displayLabel;
    }
}
