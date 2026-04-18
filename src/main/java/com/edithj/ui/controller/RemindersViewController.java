package com.edithj.ui.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.edithj.ui.model.ReminderViewModel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

@SuppressWarnings("unused")
public class RemindersViewController {

    @FXML
    private ListView<ReminderViewModel> remindersList;
    @FXML
    private Label countLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label hintLabel;
    @FXML
    private Button btnFilterAll;
    @FXML
    private Button btnFilterToday;
    @FXML
    private Button btnFilterUpcoming;
    @FXML
    private Button btnFilterCompleted;

    private final RemindersController remindersController;
    private FilterMode currentFilter = FilterMode.ALL;

    enum FilterMode {
        ALL, TODAY, UPCOMING, COMPLETED
    }

    public RemindersViewController() {
        this(new RemindersController());
    }

    public RemindersViewController(RemindersController remindersController) {
        this.remindersController = remindersController;
    }

    @FXML
    private void initialize() {
        remindersList.setCellFactory(listView -> new ReminderCell(this::onReminderDone, this::onReminderSnooze, this::onReminderDelete));
        filterAll();
    }

    @FXML
    private void filterAll() {
        currentFilter = FilterMode.ALL;
        updateFilterButtons();
        loadReminders(remindersController.listReminders().stream()
                .filter(r -> !r.isCompleted())
                .collect(Collectors.toList()));
        setStatus("Showing all active reminders");
    }

    @FXML
    private void filterToday() {
        currentFilter = FilterMode.TODAY;
        updateFilterButtons();
        LocalDate today = LocalDate.now();
        List<ReminderViewModel> todayReminders = remindersController.listReminders().stream()
                .filter(r -> !r.isCompleted())
                .filter(r -> isToday(r.getDueAt()))
                .collect(Collectors.toList());
        loadReminders(todayReminders);
        setStatus("Showing today's reminders (" + todayReminders.size() + ")");
    }

    @FXML
    private void filterUpcoming() {
        currentFilter = FilterMode.UPCOMING;
        updateFilterButtons();
        LocalDate today = LocalDate.now();
        List<ReminderViewModel> upcomingReminders = remindersController.listReminders().stream()
                .filter(r -> !r.isCompleted())
                .filter(r -> isAfterToday(r.getDueAt()))
                .collect(Collectors.toList());
        loadReminders(upcomingReminders);
        setStatus("Showing upcoming reminders (" + upcomingReminders.size() + ")");
    }

    @FXML
    private void filterCompleted() {
        currentFilter = FilterMode.COMPLETED;
        updateFilterButtons();
        List<ReminderViewModel> completedReminders = remindersController.listReminders().stream()
                .filter(ReminderViewModel::isCompleted)
                .collect(Collectors.toList());
        loadReminders(completedReminders);
        setStatus("Showing completed reminders (" + completedReminders.size() + ")");
    }

    private void loadReminders(List<ReminderViewModel> reminders) {
        remindersList.setItems(FXCollections.observableArrayList(reminders));
        countLabel.setText(reminders.size() + " reminder" + (reminders.size() == 1 ? "" : "s"));
    }

    @FXML
    private void onNewReminder() {
        TextInputDialog descriptionDialog = new TextInputDialog();
        descriptionDialog.setTitle("New Reminder");
        descriptionDialog.setHeaderText("Create a new reminder");
        descriptionDialog.setContentText("Reminder text:");
        descriptionDialog.setWidth(450);

        String description = descriptionDialog.showAndWait().orElse(null);
        if (description == null || description.trim().isEmpty()) {
            return;
        }

        TextInputDialog timeDialog = new TextInputDialog();
        timeDialog.setTitle("When?");
        timeDialog.setHeaderText("When should this reminder be due?");
        timeDialog.setContentText("Time hint (e.g., 'in 10 minutes', 'tomorrow at 3pm', '5pm'):");
        timeDialog.setWidth(450);

        String timeHint = timeDialog.showAndWait().orElse(null);
        if (timeHint == null || timeHint.trim().isEmpty()) {
            return;
        }

        try {
            remindersController.createReminder(description.trim(), timeHint.trim());
            setStatus("Reminder created successfully");
            filterAll(); // Reload all reminders
        } catch (IllegalArgumentException e) {
            setStatus("Error creating reminder: " + e.getMessage());
        }
    }

    private void onReminderDone(ReminderViewModel reminder) {
        remindersController.markReminderDone(reminder.getId());
        setStatus("Reminder marked as done");
        applyCurrentFilter();
    }

    private void onReminderSnooze(ReminderViewModel reminder) {
        TextInputDialog snoozeDialog = new TextInputDialog("5");
        snoozeDialog.setTitle("Snooze");
        snoozeDialog.setHeaderText("Snooze this reminder");
        snoozeDialog.setContentText("Minutes to snooze:");
        snoozeDialog.setWidth(300);

        String result = snoozeDialog.showAndWait().orElse(null);
        if (result != null && !result.trim().isEmpty()) {
            try {
                int minutes = Integer.parseInt(result.trim());
                remindersController.snoozeReminder(reminder.getId(), minutes);
                setStatus("Reminder snoozed for " + minutes + " minutes");
                applyCurrentFilter();
            } catch (NumberFormatException e) {
                setStatus("Error: Please enter a valid number");
            }
        }
    }

    private void onReminderDelete(ReminderViewModel reminder) {
        remindersController.deleteReminder(reminder.getId());
        setStatus("Reminder deleted");
        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        Platform.runLater(() -> {
            switch (currentFilter) {
                case ALL -> filterAll();
                case TODAY -> filterToday();
                case UPCOMING -> filterUpcoming();
                case COMPLETED -> filterCompleted();
            }
        });
    }

    private void updateFilterButtons() {
        for (Button btn : new Button[]{btnFilterAll, btnFilterToday, btnFilterUpcoming, btnFilterCompleted}) {
            btn.getStyleClass().removeAll("filter-tab-button", "filter-tab-button-active");
            btn.getStyleClass().add("filter-tab-button");
        }

        switch (currentFilter) {
            case ALL -> btnFilterAll.getStyleClass().add("filter-tab-button-active");
            case TODAY -> btnFilterToday.getStyleClass().add("filter-tab-button-active");
            case UPCOMING -> btnFilterUpcoming.getStyleClass().add("filter-tab-button-active");
            case COMPLETED -> btnFilterCompleted.getStyleClass().add("filter-tab-button-active");
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private boolean isToday(String dueAtStr) {
        try {
            LocalDate dueDate = parseDate(dueAtStr);
            return dueDate.equals(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAfterToday(String dueAtStr) {
        try {
            LocalDate dueDate = parseDate(dueAtStr);
            return dueDate.isAfter(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private LocalDate parseDate(String dateTimeStr) {
        // Parse format like "2026-04-18 15:30"
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDate.now();
        }
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.replace(" ", "T").substring(0, 16));
        return dateTime.toLocalDate();
    }

    /**
     * Custom ListCell for displaying reminders with inline action buttons.
     */
    private static class ReminderCell extends ListCell<ReminderViewModel> {
        private final VBox container;
        private final Label textLabel;
        private final Label dueLabel;
        private final Label statusBadge;
        private final Button doneButton;
        private final Button snoozeButton;
        private final Button deleteButton;

        private final Runnable onDone;
        private final Runnable onSnooze;
        private final Runnable onDelete;
    private final java.util.function.Consumer<ReminderViewModel> onDoneConsumer;
    private final java.util.function.Consumer<ReminderViewModel> onSnoozeConsumer;
    private final java.util.function.Consumer<ReminderViewModel> onDeleteConsumer;

        ReminderCell(java.util.function.Consumer<ReminderViewModel> onDone,
                     java.util.function.Consumer<ReminderViewModel> onSnooze,
                     java.util.function.Consumer<ReminderViewModel> onDelete) {
            this.onDone = null; // Not used directly
            this.onSnooze = null; // Not used directly
            this.onDelete = null; // Not used directly
            this.onDoneConsumer = onDone;
            this.onSnoozeConsumer = onSnooze;
            this.onDeleteConsumer = onDelete;

            container = new VBox();
            container.setStyle("-fx-padding: 10px; -fx-border-bottom: 1px solid rgba(14, 165, 233, 0.1);");
            container.setSpacing(6.0);

            HBox titleRow = new HBox();
            titleRow.setSpacing(8.0);
            titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            textLabel = new Label();
            textLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #bae6fd;");
            textLabel.setWrapText(false);

            statusBadge = new Label();
            statusBadge.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 999; -fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399; -fx-border-color: #34d399; -fx-border-width: 1;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            titleRow.getChildren().addAll(textLabel, statusBadge, spacer);

            dueLabel = new Label();
            dueLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7dd3fc; -fx-opacity: 0.8;");

            HBox buttonRow = new HBox();
            buttonRow.setSpacing(6.0);
            buttonRow.setStyle("-fx-padding: 6 0 0 0;");

            doneButton = new Button("Done");
            doneButton.setStyle("-fx-font-size: 10px; -fx-padding: 4 10;");
            doneButton.setOnAction(e -> onDoneConsumer.accept(getItem()));
            doneButton.getStyleClass().add("mini-action-button");

            snoozeButton = new Button("Snooze");
            snoozeButton.setStyle("-fx-font-size: 10px; -fx-padding: 4 10;");
            snoozeButton.setOnAction(e -> onSnoozeConsumer.accept(getItem()));
            snoozeButton.getStyleClass().add("mini-action-button");

            deleteButton = new Button("Delete");
            deleteButton.setStyle("-fx-font-size: 10px; -fx-padding: 4 10;");
            deleteButton.setOnAction(e -> onDeleteConsumer.accept(getItem()));
            deleteButton.getStyleClass().add("mini-action-button");

            buttonRow.getChildren().addAll(doneButton, snoozeButton, deleteButton);

            container.getChildren().addAll(titleRow, dueLabel, buttonRow);
            setGraphic(container);
        }

        @Override
        protected void updateItem(ReminderViewModel reminder, boolean empty) {
            super.updateItem(reminder, empty);
            if (empty || reminder == null) {
                setGraphic(null);
                setText(null);
            } else {
                textLabel.setText(reminder.getText());
                dueLabel.setText("Due: " + reminder.getDueAt());
                statusBadge.setText(reminder.isCompleted() ? "DONE" : "ACTIVE");
                setGraphic(container);
            }
        }
    }
}



