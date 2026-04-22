package com.edithj.ui.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.edithj.assistant.AssistantResponse;
import com.edithj.ui.model.ChatMessageViewModel;
import com.edithj.ui.session.UiAssistantGateway;
import com.edithj.ui.session.UiPreferencesService;
import com.edithj.ui.session.UiSpeechService;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class ChatController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Logger LOGGER = Logger.getLogger(ChatController.class.getName());

    @FXML
    private ListView<ChatMessageViewModel> messagesList;
    @FXML
    private TextArea messageInput;
    @FXML
    private Button btnMic;
    @FXML
    private Label micStatusLabel;

    private final ObservableList<ChatMessageViewModel> messages = FXCollections.observableArrayList();
    private final UiAssistantGateway assistantGateway;
    private final UiSpeechService uiSpeechService;
    private final UiPreferencesService uiPreferencesService = UiPreferencesService.instance();

    private static final String MIC_HINT_IDLE = "Tap to speak";
    private static final String MIC_HINT_LISTENING = "Listening...";
    private static final String MIC_HINT_ERROR = "Didn't catch that, try again.";
    private static final String MIC_HINT_UNAVAILABLE = "Microphone unavailable";

    private boolean micErrorState;

    public ChatController() {
        this(UiAssistantGateway.instance(), UiSpeechService.instance());
    }

    public ChatController(UiAssistantGateway assistantGateway) {
        this(assistantGateway, UiSpeechService.instance());
    }

    public ChatController(UiAssistantGateway assistantGateway, UiSpeechService uiSpeechService) {
        this.assistantGateway = assistantGateway;
        this.uiSpeechService = uiSpeechService;
    }

    @FXML
    private void initialize() {
        messagesList.setItems(messages);
        messagesList.setCellFactory(listView -> new MessageCell());

        // Ctrl+Enter to send, plain Enter = newline
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                e.consume();
                sendMessage();
            }
        });

        if (!btnMic.getStyleClass().contains("mic-button")) {
            btnMic.getStyleClass().add("mic-button");
        }
        btnMic.setOnAction(event -> onMicClicked());
        if (!uiSpeechService.isAvailable()) {
            btnMic.setDisable(true);
            btnMic.setTooltip(new Tooltip("Microphone not available"));
            micStatusLabel.setText(MIC_HINT_UNAVAILABLE);
        } else {
            micStatusLabel.setText(MIC_HINT_IDLE);
        }
        updateMicVisualState();

        messages.add(new ChatMessageViewModel(
                "SYSTEM_CORE",
                "EDITH Initialization Complete. All systems nominal. How may I assist?",
                currentTime()
        ));
    }

    @FXML
    private void sendMessage() {
        String text = messageInput.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        String normalized = text.trim();
        messages.add(new ChatMessageViewModel("user", normalized, currentTime()));
        messageInput.clear();

        messagesList.scrollTo(messages.size() - 1);

        assistantGateway.executeAsync(normalized, this::appendAssistantMessage, throwable -> {
            messages.add(new ChatMessageViewModel("assistant", "I hit an error while handling that request.", currentTime()));
            messagesList.scrollTo(messages.size() - 1);
        });
    }

    @FXML
    private void insertSetReminderTemplate() {
        insertTemplate("remind me to  at ");
    }

    @FXML
    private void insertAddNoteTemplate() {
        insertTemplate("note ");
    }

    @FXML
    private void insertWorkModeTemplate() {
        insertTemplate("start work mode");
    }

    @FXML
    private void insertToolsTemplate() {
        insertTemplate("open desktop tools");
    }

    @FXML
    private void insertHelpTemplate() {
        insertTemplate("what can you do?");
    }

    private void appendAssistantMessage(AssistantResponse response) {
        messages.add(new ChatMessageViewModel("assistant", response.answer(), currentTime()));
        messagesList.scrollTo(messages.size() - 1);
    }

    private void onMicClicked() {
        if (!uiSpeechService.isListening()) {
            startVoiceInput();
            return;
        }
        stopVoiceInput();
    }

    private void startVoiceInput() {
        micErrorState = false;
        micStatusLabel.setText(MIC_HINT_LISTENING);
        updateMicVisualState();
        uiSpeechService.startListening(
                this::handleRecognizedText,
                this::showVoiceError
        );
        updateMicVisualState();
    }

    private void stopVoiceInput() {
        uiSpeechService.stopListening();
        micErrorState = false;
        micStatusLabel.setText(MIC_HINT_IDLE);
        updateMicVisualState();
    }

    private void handleRecognizedText(String text) {
        micErrorState = false;
        micStatusLabel.setText(MIC_HINT_IDLE);
        updateMicVisualState();

        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank()) {
            showVoiceError(new IllegalStateException("No speech recognized"));
            return;
        }

        messageInput.setText(normalized);
        if (uiPreferencesService.isAutoSendVoiceInputEnabled()) {
            sendMessage();
            return;
        }

        messageInput.requestFocus();
        messageInput.positionCaret(messageInput.getText().length());
    }

    private void showVoiceError(Throwable error) {
        LOGGER.log(Level.WARNING, "Voice input failed", error);
        messages.add(new ChatMessageViewModel(
                "SYSTEM_CORE",
                "I couldn't hear that clearly. Please try again.",
                currentTime()
        ));
        messagesList.scrollTo(messages.size() - 1);

        micErrorState = true;
        micStatusLabel.setText(MIC_HINT_ERROR);
        updateMicVisualState();

        PauseTransition clearError = new PauseTransition(Duration.seconds(1.8));
        clearError.setOnFinished(event -> {
            micErrorState = false;
            if (uiSpeechService.isAvailable()) {
                micStatusLabel.setText(MIC_HINT_IDLE);
            }
            updateMicVisualState();
        });
        clearError.play();
    }

    private void updateMicVisualState() {
        btnMic.getStyleClass().removeAll("mic-button-listening", "mic-button-error");
        if (uiSpeechService.isListening()) {
            if (!btnMic.getStyleClass().contains("mic-button-listening")) {
                btnMic.getStyleClass().add("mic-button-listening");
            }
            return;
        }
        if (micErrorState && !btnMic.getStyleClass().contains("mic-button-error")) {
            btnMic.getStyleClass().add("mic-button-error");
        }
    }

    private void insertTemplate(String template) {
        messageInput.setText(template);
        messageInput.requestFocus();
        messageInput.positionCaret(template.length());
    }

    private String currentTime() {
        return LocalTime.now().format(TIME_FORMAT);
    }

    /* ── Inner Cell Class ─────────────────────────────── */
    private static class MessageCell extends ListCell<ChatMessageViewModel> {

        @Override
        protected void updateItem(ChatMessageViewModel item, boolean empty) {
            super.updateItem(item, empty);
            setBackground(null);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            boolean isUser = "user".equalsIgnoreCase(item.getRole());
            boolean isSystem = item.getRole().toUpperCase().contains("SYSTEM");

            // -- Sender row (role + timestamp) -----------
            String senderTag = isSystem ? "⚙  " + item.getRole().toUpperCase()
                    : isUser ? "▸  YOU" : "▸  EDITH";
            Label senderLabel = new Label(senderTag + "   " + item.getTimestamp());
            senderLabel.getStyleClass().add(isSystem ? "msg-sender-system"
                    : isUser ? "msg-sender-user" : "msg-sender-ai");

            // -- Message content -------------------------
            final String fullText = item.getMessage();
            Label messageLabel = new Label(isUser || isSystem ? fullText : "");
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(560);
            messageLabel.getStyleClass().add(isSystem ? "msg-text-system"
                    : isUser ? "msg-text-user" : "msg-text-ai");

            // -- Bubble container ------------------------
            VBox bubble = new VBox(5, senderLabel, messageLabel);
            bubble.getStyleClass().add(isSystem ? "msg-system-bubble"
                    : isUser ? "msg-user-bubble" : "msg-ai-bubble");
            bubble.setMaxWidth(580);

            // -- Outer row layout for alignment ----------
            HBox row = new HBox();
            row.setPadding(new Insets(6, 12, 6, 12));

            if (isSystem) {
                row.setAlignment(Pos.CENTER);
                bubble.setMaxWidth(700);
            } else if (isUser) {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.getChildren().addAll(spacer, bubble);
            } else {
                row.getChildren().add(bubble);
            }

            setGraphic(row);
            setText(null);

            // -- Entry animations ------------------------
            FadeTransition fade = new FadeTransition(Duration.millis(380), row);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(380), row);
            slide.setFromY(isUser ? 12 : -12);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            if (!isUser && !isSystem) {
                // Typewriter effect for EDITH responses
                Transition typewriter = new Transition() {
                    {
                        setCycleDuration(Duration.millis(Math.min(1200, fullText.length() * 18L)));
                    }

                    @Override
                    protected void interpolate(double frac) {
                        int len = (int) Math.round(fullText.length() * frac);
                        messageLabel.setText(fullText.substring(0, len));
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
