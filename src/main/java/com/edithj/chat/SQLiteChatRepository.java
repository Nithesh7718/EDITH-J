package com.edithj.chat;

import com.edithj.assistant.AssistantResponse;
import com.edithj.storage.DatabaseManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SQLiteChatRepository implements ChatRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<AssistantResponse.AssistantAction>> ACTIONS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<AssistantResponse.RecoveryOption>> RECOVERY_OPTIONS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<java.util.Map<String, String>> METADATA_TYPE = new TypeReference<>() {
    };

    private final DatabaseManager databaseManager;

    public SQLiteChatRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<ChatMessage> findRecent(int limit) {
        String sql = """
                SELECT id, role, content, timestamp, source, intent_type, success, requires_approval, approval_type,
                       explanation, plan_goal, task_plan_json, actions_json, recovery_options_json, metadata_json
                FROM chat_history
                ORDER BY timestamp DESC
                LIMIT ?
                """;
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
                            resultSet.getString("explanation"),
                            resultSet.getString("plan_goal"),
                            readTaskPlan(resultSet.getString("task_plan_json"), resultSet.getString("plan_goal")),
                            readJson(resultSet.getString("actions_json"), ACTIONS_TYPE, List.of()),
                            readJson(resultSet.getString("recovery_options_json"), RECOVERY_OPTIONS_TYPE, List.of()),
                            readJson(resultSet.getString("metadata_json"), METADATA_TYPE, java.util.Map.of())
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
        String sql = """
                INSERT INTO chat_history
                (id, role, content, timestamp, source, intent_type, success, requires_approval, approval_type,
                 explanation, plan_goal, task_plan_json, actions_json, recovery_options_json, metadata_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
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
            statement.setString(11, message.planGoal());
            statement.setString(12, writeJson(message.taskPlan()));
            statement.setString(13, writeJson(message.actions()));
            statement.setString(14, writeJson(message.recoveryOptions()));
            statement.setString(15, writeJson(message.metadata()));
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

    private AssistantResponse.TaskPlan readTaskPlan(String json, String planGoal) {
        if (json == null || json.isBlank()) {
            return (planGoal == null || planGoal.isBlank()) ? null : new AssistantResponse.TaskPlan(planGoal, List.of());
        }
        try {
            return MAPPER.readValue(json, AssistantResponse.TaskPlan.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse stored task plan JSON", exception);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse stored chat JSON", exception);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize chat message JSON", exception);
        }
    }
}
