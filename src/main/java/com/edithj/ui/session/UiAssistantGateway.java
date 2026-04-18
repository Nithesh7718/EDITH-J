package com.edithj.ui.session;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.edithj.assistant.AssistantResponse;
import com.edithj.assistant.AssistantService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class UiAssistantGateway {

    private static final UiAssistantGateway INSTANCE = new UiAssistantGateway();

    private final AssistantService assistantService;
    private final StringProperty statusText = new SimpleStringProperty("Ready");
    private final BooleanProperty thinking = new SimpleBooleanProperty(false);

    private UiAssistantGateway() {
        this.assistantService = new AssistantService();
    }

    public static UiAssistantGateway instance() {
        return INSTANCE;
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public BooleanProperty thinkingProperty() {
        return thinking;
    }

    public AssistantResponse execute(String input) {
        String normalized = input == null ? "" : input.trim();
        if (normalized.isBlank()) {
            return assistantService.handleTypedInput("");
        }
        return assistantService.handleTypedInput(normalized);
    }

    public void executeAsync(String input, Consumer<AssistantResponse> onSuccess, Consumer<Throwable> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onError, "onError");
        setThinking("Thinking...");

        CompletableFuture
                .supplyAsync(() -> execute(input))
                .whenComplete((response, throwable) -> Platform.runLater(() -> {
                    clearThinking();
                    if (throwable != null) {
                        onError.accept(throwable);
                        return;
                    }
                    onSuccess.accept(response);
                }));
    }

    public void setReadyMessage(String message) {
        Platform.runLater(() -> statusText.set(message == null || message.isBlank() ? "Ready" : message));
    }

    private void setThinking(String text) {
        Platform.runLater(() -> {
            thinking.set(true);
            statusText.set(text);
        });
    }

    private void clearThinking() {
        thinking.set(false);
        statusText.set("Ready");
    }
}
