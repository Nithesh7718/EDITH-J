package com.edithj.ui.controller;

import com.edithj.ui.model.ReminderViewModel;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

@SuppressWarnings("unused")
public class RemindersComingSoonController {

    @FXML
    private ListView<String> remindersPreviewList;

    private final RemindersController remindersController;

    public RemindersComingSoonController() {
        this(new RemindersController());
    }

    public RemindersComingSoonController(RemindersController remindersController) {
        this.remindersController = remindersController;
    }

    @FXML
    private void initialize() {
        var latest = remindersController.listReminders().stream().limit(4).toList();
        if (latest.isEmpty()) {
            remindersPreviewList.setItems(FXCollections.observableArrayList(
                    "No reminders yet. Try: remind me to drink water at 4 PM",
                    "Upcoming reminders and snooze controls are coming soon."));
            return;
        }

        remindersPreviewList.setItems(FXCollections.observableArrayList(
                latest.stream().map(this::formatReminder).toList()));
    }

    private String formatReminder(ReminderViewModel reminder) {
        String state = reminder.isCompleted() ? "DONE" : "UPCOMING";
        return "[" + state + "] " + reminder.getText() + " at " + reminder.getDueAt();
    }
}
