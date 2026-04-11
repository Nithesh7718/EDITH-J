package com.edithj.ui.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.edithj.assistant.AssistantResponse;
import com.edithj.assistant.AssistantService;
import com.edithj.ui.model.ChatMessageViewModel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

@SuppressWarnings("unused")
public class ChatController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private ListView<ChatMessageViewModel> messagesList;

    @FXML
    private TextArea messageInput;

    private final ObservableList<ChatMessageViewModel> messages = FXCollections.observableArrayList();
    private final AssistantService assistantService;

    public ChatController() {
        this(new AssistantService());
    }

    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @FXML
    private void initialize() {
        messagesList.setItems(messages);
        messages.add(new ChatMessageViewModel("assistant", "Hello. I am ready to help.", currentTime()));
    }

    @FXML
    private void sendMessage() {
        String text = messageInput.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        messages.add(new ChatMessageViewModel("user", text.trim(), currentTime()));
        messageInput.clear();

        AssistantResponse response = assistantService.handleTypedInput(text.trim());
        messages.add(new ChatMessageViewModel("assistant", response.answer(), currentTime()));
    }

    private String currentTime() {
        return LocalTime.now().format(TIME_FORMAT);
    }
}
