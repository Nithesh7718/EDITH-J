package com.edithj.ui.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class ReminderViewModel {

    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty text = new SimpleStringProperty();
    private final StringProperty dueAt = new SimpleStringProperty();
    private final BooleanProperty completed = new SimpleBooleanProperty();

    public ReminderViewModel(String id, String text, String dueAt, boolean completed) {
        this.id.set(id);
        this.text.set(text);
        this.dueAt.set(dueAt);
        this.completed.set(completed);
    }

    // Legacy constructor for compatibility
    public ReminderViewModel(String text, String dueAt) {
        this.text.set(text);
        this.dueAt.set(dueAt);
        this.completed.set(false);
    }

    public String getId() {
        return id.get();
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getText() {
        return text.get();
    }

    public StringProperty textProperty() {
        return text;
    }

    public String getDueAt() {
        return dueAt.get();
    }

    public StringProperty dueAtProperty() {
        return dueAt;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public BooleanProperty completedProperty() {
        return completed;
    }
}
