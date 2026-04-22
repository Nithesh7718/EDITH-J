package com.edithj.ui.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import com.edithj.assistant.AssistantResponse;
import com.edithj.assistant.AssistantStatus;
import com.edithj.assistant.AssistantStatusProbe;
import com.edithj.assistant.AssistantStatusService;
import com.edithj.ui.component.AudioAuraCanvas;
import com.edithj.ui.model.AssistantUiState;
import com.edithj.ui.model.ChatMessageViewModel;
import com.edithj.ui.navigation.SceneManager;
import com.edithj.ui.session.UiAssistantGateway;
import com.edithj.ui.session.UiSpeechService;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for the FRIDAY-style main shell of EDITH-J.
 *
 * <p>Architecture overview:
 * <ul>
 *   <li>All business logic stays in service / gateway classes.</li>
 *   <li>This controller wires UI properties to observable services via bindings.</li>
 *   <li>State transitions are driven through {@link UiAssistantGateway}.</li>
 *   <li>The audio aura responds to {@link UiAssistantGateway#audioInputLevelProperty()}.</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class MainShellController {

    private static final DateTimeFormatter TIME_FORMAT  = DateTimeFormatter.ofPattern("HH:mm");
    private static final String MIC_HINT_IDLE           = "Tap 🎙 or press Space to speak";
    private static final String MIC_HINT_LISTENING      = "Listening… speak now";
    private static final String MIC_HINT_ERROR          = "Didn't catch that — please try again";
    private static final String MIC_HINT_UNAVAILABLE    = "Microphone unavailable";

    // ── FXML injections ───────────────────────────────────────────────────────
    @FXML private StackPane     auraHost;
    @FXML private StackPane     contentHost;

    // Status bar
    @FXML private Label         statePillLabel;
    @FXML private Label         connectionLabel;
    @FXML private Label         modelBadgeLabel;

    // Left panel
    @FXML private Label         stateCardLabel;
    @FXML private Label         modelCardLabel;
    @FXML private Label         lastCommandLabel;

    // Nav buttons
    @FXML private Button        btnChat;
    @FXML private Button        btnNotes;
    @FXML private Button        btnReminders;
    @FXML private Button        btnDesktopTools;
    @FXML private Button        btnSettings;

    // Transcript
    @FXML private ListView<ChatMessageViewModel> transcriptList;
    @FXML private HBox          partialBar;
    @FXML private Label         partialLabel;

    // Control bar
    @FXML private Button        btnMic;
    @FXML private TextField     textInput;
    @FXML private Button        btnSend;
    @FXML private Label         micHintLabel;

    // ── Services ──────────────────────────────────────────────────────────────
    private final UiAssistantGateway    gateway         = UiAssistantGateway.instance();
    private final AssistantStatusService statusService  = AssistantStatusService.instance();
    private final AssistantStatusProbe  statusProbe     = new AssistantStatusProbe();
    private final SceneManager          sceneManager    = new SceneManager();
    private final UiSpeechService       speechService;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final ObservableList<ChatMessageViewModel> messages = FXCollections.observableArrayList();

    /** The live audio aura canvas — created in #initialize for access to scene size. */
    private AudioAuraCanvas auraCanvas;

    /** Partial (still-being-spoken) transcript text — replaced on finalisation. */
    private ChatMessageViewModel partialMessage = null;

    private boolean micErrorState = false;

    // ── Constructor ───────────────────────────────────────────────────────────
    public MainShellController() {
        this.speechService = UiSpeechService.instance();
    }

    // ── Initialise ────────────────────────────────────────────────────────────
    @FXML
    private void initialize() {
        buildAuraCanvas();
        wireTranscript();
        wireStatePill();
        wireKeyboardShortcuts();
        showChatView();
        appendSystemMessage("EDITH Initialization Complete. All systems nominal.");
        runStartupStatusProbe();
    }

    // ── Aura canvas ───────────────────────────────────────────────────────────
    private void buildAuraCanvas() {
        auraCanvas = new AudioAuraCanvas(320);
        // Bind canvas size to container so it stays centred
        auraCanvas.widthProperty().bind(auraHost.widthProperty());
        auraCanvas.heightProperty().bind(auraHost.heightProperty());
        // Wire audio level & state from gateway
        auraCanvas.audioLevelProperty().bind(gateway.audioInputLevelProperty());
        auraCanvas.uiStateProperty().bind(gateway.uiStateProperty());
        auraHost.getChildren().add(auraCanvas);
    }

    // ── Transcript wiring ──────────────────────────────────────────────────────
    private void wireTranscript() {
        transcriptList.setItems(messages);
        transcriptList.setCellFactory(lv -> new TranscriptCell());
    }

    // ── State pill binding ─────────────────────────────────────────────────────
    private void wireStatePill() {
        gateway.uiStateProperty().addListener((obs, oldState, newState) ->
                Platform.runLater(() -> updateStatePill(newState)));
        updateStatePill(gateway.getUiState());

        // Also map state to left-panel card
        gateway.uiStateProperty().addListener((obs, oldState, newState) ->
                Platform.runLater(() -> {
                    stateCardLabel.setText(newState.name());
                    stateCardLabel.getStyleClass().setAll("card-value-cyan");
                }));
    }

    private void updateStatePill(AssistantUiState state) {
        statePillLabel.setText(state.displayLabel());
        statePillLabel.getStyleClass().setAll("state-pill");
        statePillLabel.getStyleClass().add(switch (state) {
            case IDLE       -> "state-pill-idle";
            case LISTENING  -> "state-pill-listening";
            case PROCESSING -> "state-pill-processing";
            case SPEAKING   -> "state-pill-speaking";
        });
    }

    // ── Keyboard shortcuts ────────────────────────────────────────────────────
    private void wireKeyboardShortcuts() {
        // Space bar in text input is intentional typing — don't intercept there.
        // Intercept at root pane level only when input is NOT focused.
        auraHost.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                e.consume();
                onMicToggle();
            }
        });
    }

    // ── Navigation actions ────────────────────────────────────────────────────
    @FXML
    private void showChatView() {
        setActiveNav(btnChat);
        loadContent("/fxml/chat-view.fxml");
    }

    @FXML
    private void showNotesView() {
        setActiveNav(btnNotes);
        loadContent("/fxml/notes-view.fxml");
    }

    @FXML
    private void showRemindersView() {
        setActiveNav(btnReminders);
        loadContent("/fxml/reminders-view.fxml");
    }

    @FXML
    private void showDesktopToolsView() {
        setActiveNav(btnDesktopTools);
        loadContent("/fxml/desktop-tools-view.fxml");
    }

    @FXML
    private void showSettingsView() {
        setActiveNav(btnSettings);
        loadContent("/fxml/settings-view.fxml");
    }

    private void setActiveNav(Button selected) {
        Button[] navButtons = {btnChat, btnNotes, btnReminders, btnDesktopTools, btnSettings};
        for (Button btn : navButtons) {
            btn.getStyleClass().removeAll("nav-button", "nav-button-active", "nav-button-muted");
            btn.getStyleClass().add(btn == selected ? "nav-button-active" : "nav-button-muted");
        }
    }

    private void loadContent(String resourcePath) {
        Node content = sceneManager.loadView(resourcePath);
        if (content == null) return;

        contentHost.getChildren().setAll(content);
        content.setOpacity(0);
        content.setTranslateY(18);

        FadeTransition fade = new FadeTransition(Duration.millis(400), content);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(400), content);
        slide.setFromY(18);
        slide.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(400), content);
        scale.setFromX(0.97);
        scale.setFromY(0.97);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition entry = new ParallelTransition(fade, slide, scale);
        entry.setInterpolator(Interpolator.SPLINE(0.2, 0.0, 0.2, 1.0));
        entry.play();
    }

    // ── Mic toggle ────────────────────────────────────────────────────────────
    @FXML
    private void onMicToggle() {
        if (!speechService.isAvailable()) {
            micHintLabel.setText(MIC_HINT_UNAVAILABLE);
            showToast("Microphone unavailable");
            return;
        }

        if (speechService.isListening()) {
            stopVoiceInput();
        } else {
            startVoiceInput();
        }
    }

    private void startVoiceInput() {
        micErrorState = false;
        gateway.setListening();
        micHintLabel.setText(MIC_HINT_LISTENING);
        updateMicButton();

        speechService.startListening(
            this::handleRecognisedSpeech,
            this::handleSpeechError
        );
    }

    private void stopVoiceInput() {
        speechService.stopListening();
        micErrorState = false;
        gateway.setIdle();
        micHintLabel.setText(MIC_HINT_IDLE);
        updateMicButton();
    }

    private void handleRecognisedSpeech(String text) {
        micErrorState = false;
        gateway.setIdle();
        micHintLabel.setText(MIC_HINT_IDLE);
        updateMicButton();

        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank()) {
            handleSpeechError(new IllegalStateException("No speech recognized"));
            return;
        }

        appendUserMessage(normalized);
        lastCommandLabel.setText(normalized.length() > 40
                ? normalized.substring(0, 37) + "…" : normalized);
        dispatchToAssistant(normalized);
    }

    private void handleSpeechError(Throwable err) {
        micErrorState = true;
        gateway.setIdle();
        micHintLabel.setText(MIC_HINT_ERROR);
        updateMicButton();
        appendSystemMessage("Couldn't hear that clearly — please try again.");

        PauseTransition reset = new PauseTransition(Duration.seconds(2));
        reset.setOnFinished(e -> {
            micErrorState = false;
            micHintLabel.setText(MIC_HINT_IDLE);
            updateMicButton();
        });
        reset.play();
    }

    private void updateMicButton() {
        btnMic.getStyleClass().removeAll(
                "mic-button-listening", "mic-button-error", "mic-button-idle");
        btnMic.setText(speechService.isListening() ? "◉" : "🎙");
        if (speechService.isListening()) {
            btnMic.getStyleClass().add("mic-button-listening");
        } else if (micErrorState) {
            btnMic.getStyleClass().add("mic-button-error");
        } else {
            btnMic.getStyleClass().add("mic-button-idle");
        }
    }

    // ── Text input ────────────────────────────────────────────────────────────
    @FXML
    private void onTextSend() {
        String text = textInput.getText();
        if (text == null || text.isBlank()) return;

        String normalized = text.trim();
        textInput.clear();
        appendUserMessage(normalized);
        lastCommandLabel.setText(normalized.length() > 40
                ? normalized.substring(0, 37) + "…" : normalized);
        dispatchToAssistant(normalized);
    }

    // ── Quick-chip handlers ───────────────────────────────────────────────────
    @FXML private void insertWeatherChip()  { fillInput("What's the weather today?"); }
    @FXML private void insertYoutubeChip()  { fillInput("Open YouTube"); }
    @FXML private void insertReminderChip() { fillInput("Remind me to "); }
    @FXML private void insertNotesChip()    { fillInput("List my notes"); }

    private void fillInput(String text) {
        textInput.setText(text);
        textInput.requestFocus();
        textInput.positionCaret(text.length());
    }

    // ── Transcript management ─────────────────────────────────────────────────
    @FXML
    private void clearTranscript() {
        messages.clear();
        appendSystemMessage("Transcript cleared.");
    }

    private void appendUserMessage(String text) {
        messages.add(new ChatMessageViewModel("user", text, now()));
        scrollToBottom();
    }

    private void appendAssistantMessage(AssistantResponse response) {
        gateway.setSpeaking();
        messages.add(new ChatMessageViewModel("assistant", response.answer(), now()));
        scrollToBottom();

        // End speaking state after a short delay (simulate TTS duration)
        PauseTransition speakingDelay = new PauseTransition(Duration.millis(1200));
        speakingDelay.setOnFinished(e -> gateway.setIdle());
        speakingDelay.play();
    }

    private void appendSystemMessage(String text) {
        messages.add(new ChatMessageViewModel("SYSTEM_CORE", text, now()));
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) {
            transcriptList.scrollTo(messages.size() - 1);
        }
    }

    // ── Assistant dispatch ────────────────────────────────────────────────────
    private void dispatchToAssistant(String input) {
        gateway.executeAsync(
            input,
            this::appendAssistantMessage,
            err -> {
                appendSystemMessage("Error: " + err.getMessage());
                gateway.setIdle();
            }
        );
    }

    // ── Settings & Help dialogs ───────────────────────────────────────────────
    @FXML
    private void openSettingsDialog() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("EDITH-J Settings");
        alert.setHeaderText("Configuration");
        alert.setContentText("API key status: "
                + (statusService.status() == AssistantStatus.ONLINE ? "Online" : "Offline / unconfigured")
                + "\nOpen the Settings page from the left panel for full options.");
        alert.showAndWait();
    }

    // ── Toast notification ────────────────────────────────────────────────────
    private void showToast(String message) {
        if (contentHost == null) return;

        Label toast = new Label(message);
        toast.getStyleClass().add("toast-label");
        toast.setOpacity(0);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        StackPane.setMargin(toast, new Insets(16, 0, 0, 0));

        contentHost.getChildren().add(toast);

        Timeline in = new Timeline(
            new KeyFrame(Duration.millis(300), new KeyValue(toast.opacityProperty(), 1.0))
        );
        Timeline out = new Timeline(
            new KeyFrame(Duration.millis(300), new KeyValue(toast.opacityProperty(), 0.0))
        );
        PauseTransition hold = new PauseTransition(Duration.seconds(2.2));
        hold.setOnFinished(e -> out.play());
        out.setOnFinished(e -> contentHost.getChildren().remove(toast));
        in.play();
        in.setOnFinished(e -> hold.play());
    }

    // ── Startup status probe ──────────────────────────────────────────────────
    private void runStartupStatusProbe() {
        CompletableFuture.runAsync(() -> {
            statusProbe.runStartupProbe();
            Platform.runLater(() -> {
                AssistantStatus status = statusService.status();
                boolean online = status == AssistantStatus.ONLINE;
                connectionLabel.setText(online ? "Online" : "Offline");
                connectionLabel.getStyleClass().setAll(
                        online ? "conn-text" : "conn-text-offline");
                // Update model badge
                modelBadgeLabel.setText("MODEL: Groq / Llama");
                modelCardLabel.setText("Groq / Llama 3");
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String now() {
        return LocalTime.now().format(TIME_FORMAT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Inner class: TranscriptCell
    // ═══════════════════════════════════════════════════════════════════════════
    private static class TranscriptCell extends ListCell<ChatMessageViewModel> {

        @Override
        protected void updateItem(ChatMessageViewModel item, boolean empty) {
            super.updateItem(item, empty);
            setBackground(null);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            boolean isUser   = "user".equalsIgnoreCase(item.getRole());
            boolean isSystem = item.getRole().toUpperCase().contains("SYSTEM");

            // ── Sender tag ────────────────────────────────────────────────────
            String senderTag = isSystem ? "⚙  SYSTEM"
                    : isUser ? "▸  YOU" : "▸  EDITH";
            Label senderLabel = new Label(senderTag + "   " + item.getTimestamp());
            senderLabel.getStyleClass().add(isSystem ? "msg-sender-system"
                    : isUser ? "msg-sender-user" : "msg-sender-ai");

            // ── Message body ──────────────────────────────────────────────────
            final String fullText = item.getMessage();
            Label msgLabel = new Label(isUser || isSystem ? fullText : "");
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(300);
            msgLabel.getStyleClass().add(isSystem ? "msg-text-system"
                    : isUser ? "msg-text-user" : "msg-text-ai");

            // ── Bubble ────────────────────────────────────────────────────────
            VBox bubble = new VBox(4, senderLabel, msgLabel);
            bubble.getStyleClass().add(isSystem ? "msg-system-bubble"
                    : isUser ? "msg-user-bubble" : "msg-ai-bubble");
            bubble.setMaxWidth(310);

            // ── Row layout ────────────────────────────────────────────────────
            HBox row = new HBox();
            row.setPadding(new Insets(5, 10, 5, 10));

            if (isSystem) {
                row.setAlignment(Pos.CENTER);
                bubble.setMaxWidth(Double.MAX_VALUE);
            } else if (isUser) {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.getChildren().addAll(spacer, bubble);
            } else {
                row.getChildren().add(bubble);
            }

            setGraphic(row);
            setText(null);

            // ── Entry animation ───────────────────────────────────────────────
            FadeTransition fade = new FadeTransition(Duration.millis(320), row);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(320), row);
            slide.setFromY(isUser ? 10 : -10);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            if (!isUser && !isSystem) {
                // Typewriter for EDITH responses
                javafx.animation.Transition typewriter = new javafx.animation.Transition() {
                    { setCycleDuration(Duration.millis(Math.min(1000, fullText.length() * 16L))); }
                    @Override protected void interpolate(double frac) {
                        int len = (int) Math.round(fullText.length() * frac);
                        msgLabel.setText(fullText.substring(0, len));
                    }
                };
                typewriter.setInterpolator(Interpolator.LINEAR);
                new ParallelTransition(fade, slide, typewriter).play();
            } else {
                new ParallelTransition(fade, slide).play();
            }
        }
    }
}
