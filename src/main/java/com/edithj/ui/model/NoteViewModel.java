package com.edithj.ui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class NoteViewModel {

    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty content = new SimpleStringProperty();
    private final StringProperty updatedAt = new SimpleStringProperty();

    public NoteViewModel(String id, String title, String content, String updatedAt) {
        this.id.set(id);
        this.title.set(title);
        this.content.set(content);
        this.updatedAt.set(updatedAt);
    }

    public String getId() {
        return id.get();
    }

    public StringProperty idProperty() {
        return id;
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

    public String getUpdatedAt() {
        return updatedAt.get();
    }

    public StringProperty updatedAtProperty() {
        return updatedAt;
    }
}
