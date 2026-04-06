package com.edithj.ui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ReminderViewModel {

    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty dueAt = new SimpleStringProperty();

    public ReminderViewModel(String title, String dueAt) {
        this.title.set(title);
        this.dueAt.set(dueAt);
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getDueAt() {
        return dueAt.get();
    }

    public StringProperty dueAtProperty() {
        return dueAt;
    }
}
