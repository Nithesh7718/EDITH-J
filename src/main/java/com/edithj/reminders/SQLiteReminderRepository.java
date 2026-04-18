package com.edithj.reminders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.edithj.storage.DatabaseManager;

public class SQLiteReminderRepository implements ReminderRepository {

    private final DatabaseManager databaseManager;

    public SQLiteReminderRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<Reminder> findAll() {
        String sql = "SELECT id, reminder_text, due_at, is_completed, created_at, updated_at FROM reminders";
        List<Reminder> reminders = new ArrayList<>();

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                reminders.add(mapReminder(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to list reminders", exception);
        }

        return reminders;
    }

    @Override
    public Optional<Reminder> findById(String reminderId) {
        if (reminderId == null || reminderId.isBlank()) {
            return Optional.empty();
        }

        String sql = "SELECT id, reminder_text, due_at, is_completed, created_at, updated_at FROM reminders WHERE id = ?";
        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reminderId.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapReminder(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load reminder", exception);
        }

        return Optional.empty();
    }

    @Override
    public Reminder save(Reminder reminder) {
        if (reminder == null) {
            throw new IllegalArgumentException("Reminder cannot be null");
        }

        String sql = """
                INSERT INTO reminders (id, reminder_text, due_at, is_completed, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    reminder_text = excluded.reminder_text,
                    due_at = excluded.due_at,
                    is_completed = excluded.is_completed,
                    updated_at = excluded.updated_at
                """;

        Instant createdAt = reminder.getCreatedAt() == null ? Instant.now() : reminder.getCreatedAt();
        Instant updatedAt = reminder.getUpdatedAt() == null ? Instant.now() : reminder.getUpdatedAt();
        reminder.setCreatedAt(createdAt);
        reminder.setUpdatedAt(updatedAt);

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reminder.getId());
            statement.setString(2, reminder.getText());
            statement.setString(3, reminder.getDueAt() == null ? null : reminder.getDueAt().toString());
            statement.setInt(4, reminder.isCompleted() ? 1 : 0);
            statement.setString(5, createdAt.toString());
            statement.setString(6, updatedAt.toString());
            statement.executeUpdate();
            return reminder;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save reminder", exception);
        }
    }

    @Override
    public boolean deleteById(String reminderId) {
        if (reminderId == null || reminderId.isBlank()) {
            return false;
        }

        String sql = "DELETE FROM reminders WHERE id = ?";
        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reminderId.trim());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete reminder", exception);
        }
    }

    private Reminder mapReminder(ResultSet resultSet) throws SQLException {
        String dueAtValue = resultSet.getString("due_at");
        Instant dueAt = dueAtValue == null || dueAtValue.isBlank() ? null : Instant.parse(dueAtValue);

        return new Reminder(
                resultSet.getString("id"),
                resultSet.getString("reminder_text"),
                dueAt,
                resultSet.getInt("is_completed") == 1,
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }
}
