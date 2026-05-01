package com.edithj.chat;

import com.edithj.storage.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SQLiteChatRepository implements ChatRepository {

    private final DatabaseManager databaseManager;

    public SQLiteChatRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<ChatMessage> findRecent(int limit) {
        String sql = "SELECT id, role, content, timestamp, source, intent_type, success, requires_approval, approval_type, explanation FROM chat_history ORDER BY timestamp DESC LIMIT ?";
        List<ChatMessage> messages = new ArrayList<>();

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new ChatMessage(
                            resultSet.getString("id"),
                            resultSet.getString("role"),
                            resultSet.getString("content"),
                            Instant.parse(resultSet.getString("timestamp")),
                            resultSet.getString("source"),
                            resultSet.getString("intent_type"),
                            resultSet.getInt("success") != 0,
                            resultSet.getInt("requires_approval") != 0,
                            resultSet.getString("approval_type"),
                            resultSet.getString("explanation")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to fetch recent chat messages", exception);
        }

        // Return in chronological order
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public void save(ChatMessage message) {
        String sql = "INSERT INTO chat_history (id, role, content, timestamp, source, intent_type, success, requires_approval, approval_type, explanation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, message.id());
            statement.setString(2, message.role());
            statement.setString(3, message.content());
            statement.setString(4, message.timestamp().toString());
            statement.setString(5, message.source());
            statement.setString(6, message.intentType());
            statement.setInt(7, message.success() ? 1 : 0);
            statement.setInt(8, message.requiresApproval() ? 1 : 0);
            statement.setString(9, message.approvalType());
            statement.setString(10, message.explanation());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save chat message", exception);
        }
    }

    @Override
    public void clear() {
        String sql = "DELETE FROM chat_history";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to clear chat history", exception);
        }
    }
}
