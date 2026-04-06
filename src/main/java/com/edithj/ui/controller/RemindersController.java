package com.edithj.ui.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.edithj.reminders.FileReminderRepository;
import com.edithj.reminders.Reminder;
import com.edithj.reminders.ReminderService;
import com.edithj.ui.model.ReminderViewModel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class RemindersController {

    private final ReminderService reminderService;
    private final DateTimeFormatter dateTimeFormatter;

    public RemindersController() {
        this(new ReminderService(new FileReminderRepository()));
    }

    public RemindersController(ReminderService reminderService) {
        this.reminderService = reminderService;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    }

    public ObservableList<ReminderViewModel> listReminders() {
        List<Reminder> reminders = reminderService.listReminders();
        return FXCollections.observableArrayList(reminders.stream()
                .map(this::toViewModel)
                .toList());
    }

    public ObservableList<ReminderViewModel> searchReminders(String query) {
        List<Reminder> reminders = reminderService.searchReminders(query);
        return FXCollections.observableArrayList(reminders.stream()
                .map(this::toViewModel)
                .toList());
    }

    public void markReminderDone(String reminderId) {
        reminderService.markDone(reminderId);
    }

    public void deleteReminder(String reminderId) {
        reminderService.deleteReminder(reminderId);
    }

    private ReminderViewModel toViewModel(Reminder reminder) {
        String dueAtFormatted = reminder.getDueAt() != null
                ? LocalDateTime.ofInstant(reminder.getDueAt(), ZoneId.systemDefault()).format(dateTimeFormatter)
                : "unknown";
        return new ReminderViewModel(reminder.getId(), reminder.getText(), dueAtFormatted, reminder.isCompleted());
    }
}
