/* Notes list rows now use JavaFX border syntax for the item divider. */
package com.edithj.ui.controller;

import java.util.List;
import java.util.Optional;

import com.edithj.ui.model.NoteViewModel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

@SuppressWarnings("unused")
public class NotesViewController {

    @FXML
    private ListView<NoteViewModel> notesList;
    @FXML
    private TextField searchField;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentArea;
    @FXML
    private Label statusLabel;
    @FXML
    private Label hintLabel;

    private final NotesController notesController;
    private Optional<NoteViewModel> currentNote = Optional.empty();

    public NotesViewController() {
        this(new NotesController());
    }

    public NotesViewController(NotesController notesController) {
        this.notesController = notesController;
    }

    @FXML
    private void initialize() {
        // Configure list cell factory for better display
        notesList.setCellFactory(listView -> new NoteListCell());

        // Load all notes on startup
        loadAllNotes();

        // Set up list selection listener
        notesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectNote(newVal);
            }
        });

        // Set up search field listener (real-time filtering)
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                loadAllNotes();
            } else {
                searchNotes(newVal);
            }
        });
    }

    private void loadAllNotes() {
        List<NoteViewModel> notes = notesController.listNotes();
        notesList.setItems(FXCollections.observableArrayList(notes));
        clearEditor();
        setStatus("Showing " + notes.size() + " note(s)");
    }

    private void searchNotes(String query) {
        List<NoteViewModel> results = notesController.searchNotes(query);
        notesList.setItems(FXCollections.observableArrayList(results));
        clearEditor();
        setStatus("Found " + results.size() + " note(s) matching '" + query + "'");
    }

    private void selectNote(NoteViewModel note) {
        currentNote = Optional.of(note);
        titleField.setText(note.getTitle());
        contentArea.setText(note.getContent());
        setStatus("Editing: " + note.getTitle());
    }

    private void clearEditor() {
        currentNote = Optional.empty();
        titleField.clear();
        contentArea.clear();
    }

    @FXML
    private void onNewNote() {
        clearEditor();
        titleField.requestFocus();
        setStatus("Creating new note...");
    }

    @FXML
    private void onSaveNote() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        if (content.isEmpty()) {
            setStatus("Error: Note content cannot be empty");
            return;
        }

        try {
            if (currentNote.isPresent()) {
                // Update existing note
                NoteViewModel note = currentNote.get();
                Optional<NoteViewModel> updated = notesController.updateNote(note.getId(), content);
                if (updated.isPresent()) {
                    setStatus("Note updated successfully");
                    loadAllNotes();
                } else {
                    setStatus("Error: Could not update note");
                }
            } else {
                // Create new note
                NoteViewModel created = notesController.createNote(content);
                setStatus("Note created: " + created.getTitle());
                loadAllNotes();
                // Select the newly created note
                Platform.runLater(() -> {
                    notesList.getSelectionModel().select(created);
                });
            }
        } catch (IllegalArgumentException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteNote() {
        if (currentNote.isEmpty()) {
            setStatus("Error: No note selected");
            return;
        }

        String noteId = currentNote.get().getId();
        if (notesController.deleteNote(noteId)) {
            setStatus("Note deleted");
            loadAllNotes();
        } else {
            setStatus("Error: Could not delete note");
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Custom ListCell for displaying notes with title and snippet.
     */
    private static class NoteListCell extends ListCell<NoteViewModel> {

        private final VBox container;
        private final Label titleLabel;
        private final Label snippetLabel;

        NoteListCell() {
            container = new VBox();
            container.setSpacing(4.0);
            container.setStyle("-fx-padding: 8; -fx-border-color: transparent transparent rgba(14, 165, 233, 0.1) transparent; -fx-border-width: 0 0 1 0;");

            titleLabel = new Label();
            titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #bae6fd;");
            titleLabel.setWrapText(false);

            snippetLabel = new Label();
            snippetLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7dd3fc; -fx-opacity: 0.8;");
            snippetLabel.setWrapText(true);

            container.getChildren().addAll(titleLabel, snippetLabel);
            setGraphic(container);
        }

        @Override
        protected void updateItem(NoteViewModel note, boolean empty) {
            super.updateItem(note, empty);
            if (empty || note == null) {
                setGraphic(null);
                setText(null);
            } else {
                titleLabel.setText(note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle());
                String content = note.getContent();
                String snippet = content.length() > 60 ? content.substring(0, 60) + "..." : content;
                snippetLabel.setText(snippet);
                setGraphic(container);
            }
        }
    }
}
