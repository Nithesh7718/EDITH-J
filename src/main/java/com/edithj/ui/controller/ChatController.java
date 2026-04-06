package com.edithj.ui.controller;

import com.edithj.integration.llm.GroqClient;
import com.edithj.integration.llm.LlmClient;
import com.edithj.ui.model.ChatMessageViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("unused")
public class ChatController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private ListView<ChatMessageViewModel> messagesList;

    @FXML
    private TextArea messageInput;

    private final ObservableList<ChatMessageViewModel> messages = FXCollections.observableArrayList();
    private final LlmClient llmClient = new GroqClient();

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

        String reply = llmClient.generateReply(text.trim());
        messages.add(new ChatMessageViewModel("assistant", reply, currentTime()));
    }

    private String currentTime() {
        return LocalTime.now().format(TIME_FORMAT);
    }
}
