package com.edithj.ui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ChatMessageViewModel {

    private final StringProperty role = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final StringProperty timestamp = new SimpleStringProperty();

    public ChatMessageViewModel(String role, String message, String timestamp) {
        this.role.set(role);
        this.message.set(message);
        this.timestamp.set(timestamp);
    }

    public String getRole() {
        return role.get();
    }

    public StringProperty roleProperty() {
        return role;
    }

    public String getMessage() {
        return message.get();
    }

    public StringProperty messageProperty() {
        return message;
    }

    public String getTimestamp() {
        return timestamp.get();
    }

    public StringProperty timestampProperty() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + getTimestamp() + "] " + getRole() + ": " + getMessage();
    }
}
