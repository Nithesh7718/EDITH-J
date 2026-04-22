package com.edithj.ui.session;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.edithj.assistant.AssistantResponse;
import com.edithj.assistant.AssistantService;
import com.edithj.ui.model.AssistantUiState;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Application-wide singleton that bridges the {@link AssistantService} to the JavaFX UI.
 *
 * <p>All properties exposed here are safe to bind directly to UI controls and must only
 * be mutated on the JavaFX Application Thread (or via {@link Platform#runLater}).
 */
public final class UiAssistantGateway {

    private static final UiAssistantGateway INSTANCE = new UiAssistantGateway();

    private final AssistantService assistantService;

    // ── Observable properties (all must be accessed on FX thread) ────────────
    /** Plain text status shown in the status badge (e.g. "Ready", "Thinking…"). */
    private final StringProperty statusText =
            new SimpleStringProperty("Ready");

    /** True while an async assistant call is in-flight. */
    private final BooleanProperty thinking =
            new SimpleBooleanProperty(false);

    /**
     * The current UI state of the assistant voice console.
     * Consumers (aura, status pill, CSS) should bind to this.
     */
    private final ObjectProperty<AssistantUiState> uiState =
            new SimpleObjectProperty<>(AssistantUiState.IDLE);

    /**
     * Current microphone input level in [0.0, 1.0].
     * Updated by {@link UiSpeechService} or any future real-time audio meter.
     */
    private final DoubleProperty audioInputLevel =
            new SimpleDoubleProperty(0.0);

    // ────────────────────────────────────────────────────────────────────────
    private UiAssistantGateway() {
        this.assistantService = new AssistantService();
    }

    public static UiAssistantGateway instance() {
        return INSTANCE;
    }

    // ── Property accessors ────────────────────────────────────────────────────
    public StringProperty statusTextProperty()                  { return statusText; }
    public BooleanProperty thinkingProperty()                   { return thinking; }
    public ObjectProperty<AssistantUiState> uiStateProperty()   { return uiState; }
    public DoubleProperty audioInputLevelProperty()             { return audioInputLevel; }

    public AssistantUiState getUiState()                        { return uiState.get(); }
    public double           getAudioInputLevel()                { return audioInputLevel.get(); }

    // ── State mutation helpers (thread-safe) ──────────────────────────────────
    /** Transition to LISTENING state. Safe to call from any thread. */
    public void setListening() {
        runOnFx(() -> {
            uiState.set(AssistantUiState.LISTENING);
            statusText.set("Listening…");
            thinking.set(false);
        });
    }

    /** Transition to PROCESSING state. Safe to call from any thread. */
    public void setProcessing() {
        runOnFx(() -> {
            uiState.set(AssistantUiState.PROCESSING);
            statusText.set("Processing…");
            thinking.set(true);
        });
    }

    /** Transition to SPEAKING state. Safe to call from any thread. */
    public void setSpeaking() {
        runOnFx(() -> {
            uiState.set(AssistantUiState.SPEAKING);
            statusText.set("Speaking…");
            thinking.set(false);
        });
    }

    /** Transition to IDLE state. Safe to call from any thread. */
    public void setIdle() {
        runOnFx(() -> {
            uiState.set(AssistantUiState.IDLE);
            statusText.set("Ready");
            thinking.set(false);
        });
    }

    /**
     * Updates the microphone input level property.
     * Safe to call from any thread; value is clamped to [0, 1].
     */
    public void setAudioInputLevel(double level) {
        double clamped = Math.max(0.0, Math.min(1.0, level));
        runOnFx(() -> audioInputLevel.set(clamped));
    }

    // ── Assistant execution ───────────────────────────────────────────────────

    public AssistantResponse execute(String input) {
        String normalized = input == null ? "" : input.trim();
        return assistantService.handleTypedInput(normalized.isBlank() ? "" : normalized);
    }

    /**
     * Executes an assistant query asynchronously, keeping the UI state machine in sync.
     */
    public void executeAsync(String input,
                             Consumer<AssistantResponse> onSuccess,
                             Consumer<Throwable> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onError,   "onError");
        setProcessing();

        CompletableFuture
            .supplyAsync(() -> execute(input))
            .whenComplete((response, throwable) -> Platform.runLater(() -> {
                setIdle();
                if (throwable != null) {
                    onError.accept(throwable);
                    return;
                }
                onSuccess.accept(response);
            }));
    }

    /**
     * Sets a plain status text without changing the UI state machine.
     * Useful for error messages or one-off annotations.
     */
    public void setReadyMessage(String message) {
        runOnFx(() -> statusText.set(message == null || message.isBlank() ? "Ready" : message));
    }

    /** @deprecated Use {@link #setProcessing()} instead. */
    @Deprecated(since = "2.0", forRemoval = false)
    private void setThinking(String text) {
        runOnFx(() -> {
            thinking.set(true);
            statusText.set(text);
        });
    }

    /** @deprecated Use {@link #setIdle()} instead. */
    @Deprecated(since = "2.0", forRemoval = false)
    private void clearThinking() {
        thinking.set(false);
        statusText.set("Ready");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
