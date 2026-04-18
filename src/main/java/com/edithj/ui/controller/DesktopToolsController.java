package com.edithj.ui.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.assistant.AssistantResponse;
import com.edithj.ui.session.UiAssistantGateway;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;

@SuppressWarnings("unused")
public class DesktopToolsController {

    private static final Pattern TASK_PATTERN = Pattern.compile("\\[(TODO|DONE)]\\s+(\\d+)\\.\\s+(.+)");

    @FXML
    private TextField taskInput;
    @FXML
    private ListView<TaskRow> taskList;
    @FXML
    private ChoiceBox<String> focusDurationChoice;
    @FXML
    private Button focusToggleButton;
    @FXML
    private Label focusStatusLabel;
    @FXML
    private TextArea outputArea;

    private final UiAssistantGateway assistantGateway;
    private final ObservableList<TaskRow> taskRows = FXCollections.observableArrayList();
    private boolean focusActive;

    public DesktopToolsController() {
        this(UiAssistantGateway.instance());
    }

    public DesktopToolsController(UiAssistantGateway assistantGateway) {
        this.assistantGateway = assistantGateway;
    }

    @FXML
    private void initialize() {
        taskList.setItems(taskRows);
        taskList.setCellFactory(list -> new TaskRowCell());

        focusDurationChoice.setItems(FXCollections.observableArrayList("25m", "45m", "60m"));
        focusDurationChoice.setValue("25m");
        focusToggleButton.setText("Start focus");
        focusStatusLabel.setText("Not in focus mode");

        refreshTaskList();
        refreshFocusState();
    }

    @FXML
    private void onAddTask() {
        String text = taskInput.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        runCommand("add task " + text.trim(), response -> {
            taskInput.clear();
            refreshTaskList();
            appendOutput(response.answer());
        });
    }

    @FXML
    private void onToggleFocus() {
        if (!focusActive) {
            int minutes = parseMinutes(focusDurationChoice.getValue());
            runCommand("start focus " + minutes, response -> {
                appendOutput(response.answer());
                refreshFocusState();
            });
            return;
        }

        runCommand("end focus", response -> {
            appendOutput(response.answer());
            refreshFocusState();
        });
    }

    @FXML
    private void onShowClipboard() {
        runAndShow("show clipboard");
    }

    @FXML
    private void onSaveClipboardAsNote() {
        runAndShow("save clipboard as note");
    }

    @FXML
    private void onOpenDownloads() {
        runAndShow("open downloads");
    }

    @FXML
    private void onRecentFiles() {
        runAndShow("recent files");
    }

    @FXML
    private void onFindFile() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Find file");
        dialog.setHeaderText("Search in desktop files");
        dialog.setContentText("File name contains:");

        dialog.showAndWait().ifPresent(query -> {
            if (query.isBlank()) {
                return;
            }
            runAndShow("find file " + query.trim());
        });
    }

    private void runAndShow(String command) {
        runCommand(command, response -> appendOutput(response.answer()));
    }

    private void refreshTaskList() {
        runCommand("list tasks", response -> {
            taskRows.setAll(parseTaskRows(response.answer()));
            if (taskRows.isEmpty()) {
                appendOutput(response.answer());
            }
        });
    }

    private void refreshFocusState() {
        runCommand("focus status", response -> {
            String answer = response.answer();
            focusActive = !answer.toLowerCase().contains("not active");
            focusToggleButton.setText(focusActive ? "End focus" : "Start focus");

            if (focusActive) {
                focusStatusLabel.setText(answer.replace("Focus active. ", "Focused: "));
            } else {
                focusStatusLabel.setText("Not in focus mode");
            }
        });
    }

    private void runCommand(String input, java.util.function.Consumer<AssistantResponse> onSuccess) {
        assistantGateway.executeAsync(input, onSuccess, throwable -> appendOutput("Command failed: " + throwable.getMessage()));
    }

    private List<TaskRow> parseTaskRows(String answer) {
        List<TaskRow> rows = new ArrayList<>();
        String[] lines = answer.split("\\R");
        for (String line : lines) {
            Matcher matcher = TASK_PATTERN.matcher(line.trim());
            if (!matcher.matches()) {
                continue;
            }

            boolean done = "DONE".equalsIgnoreCase(matcher.group(1));
            int id = Integer.parseInt(matcher.group(2));
            String text = matcher.group(3);
            rows.add(new TaskRow(id, text, done));
        }
        return rows;
    }

    private int parseMinutes(String value) {
        if (value == null || value.isBlank()) {
            return (int) Duration.ofMinutes(25).toMinutes();
        }
        return Integer.parseInt(value.replace("m", "").trim());
    }

    private void appendOutput(String text) {
        outputArea.setText(text == null ? "" : text);
    }

    private record TaskRow(int id, String text, boolean done) {
    }

    private class TaskRowCell extends ListCell<TaskRow> {

        @Override
        protected void updateItem(TaskRow item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            Label label = new Label((item.done ? "[DONE] " : "[TODO] ") + item.id + ". " + item.text);
            label.getStyleClass().add("task-item-label");

            Button doneButton = new Button("Done");
            doneButton.getStyleClass().add("mini-action-button");
            doneButton.setDisable(item.done);
            doneButton.setOnAction(event -> runCommand("done task " + item.id, response -> {
                appendOutput(response.answer());
                refreshTaskList();
                refreshFocusState();
            }));

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().add("mini-action-button");
            removeButton.setOnAction(event -> runCommand("remove task " + item.id, response -> {
                appendOutput(response.answer());
                refreshTaskList();
            }));

            HBox row = new HBox(8, label, doneButton, removeButton);
            row.getStyleClass().add("task-row");
            setGraphic(row);
        }
    }
}
