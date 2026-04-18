package com.edithj.ui.controller;

import java.util.List;

import com.edithj.ui.model.NoteViewModel;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

@SuppressWarnings("unused")
public class NotesComingSoonController {

    @FXML
    private ListView<String> notesPreviewList;

    private final NotesController notesController;

    public NotesComingSoonController() {
        this(new NotesController());
    }

    public NotesComingSoonController(NotesController notesController) {
        this.notesController = notesController;
    }

    @FXML
    private void initialize() {
        List<NoteViewModel> latest = notesController.listNotes().stream().limit(3).toList();
        if (latest.isEmpty()) {
            notesPreviewList.setItems(FXCollections.observableArrayList(
                    "No notes yet. You can still use chat commands like: note buy milk",
                    "Pinned notes and rich editing will appear here soon."));
            return;
        }

        List<String> rows = latest.stream()
            .map(note -> note.getTitle() + " - " + note.getUpdatedAt())
            .toList();
        notesPreviewList.setItems(FXCollections.observableArrayList(rows));
    }
}
