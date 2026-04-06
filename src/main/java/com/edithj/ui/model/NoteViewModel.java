package com.edithj.ui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class NoteViewModel {

    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty content = new SimpleStringProperty();

    public NoteViewModel(String title, String content) {
        this.title.set(title);
        this.content.set(content);
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getContent() {
        return content.get();
    }

    public StringProperty contentProperty() {
        return content;
    }
}
