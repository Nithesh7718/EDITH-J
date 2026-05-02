package com.edithj.resilience;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.edithj.storage.DatabaseManager;

public final class AdaptiveFailureMemory {

    private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();
    private DatabaseManager databaseManager;

    public void initialize(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        if (databaseManager == null) {
            return;
        }

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement("SELECT failure_signature, failure_count FROM adaptive_failure_memory")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String signature = resultSet.getString("failure_signature");
                    int count = resultSet.getInt("failure_count");
                    if (signature != null && !signature.isBlank()) {
                        failureCounts.put(signature, count);
                    }
                }
            }
        } catch (SQLException ignored) {
            // best effort
        }
    }

    public int recordFailure(String signature, String recoveryHint) {
        if (signature == null || signature.isBlank()) {
            return 0;
        }

        int newValue = failureCounts.merge(signature, 1, Integer::sum);
        if (databaseManager != null) {
            try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO adaptive_failure_memory(failure_signature, failure_count, last_seen_at, recovery_hint) "
                    + "VALUES(?, ?, ?, ?) "
                    + "ON CONFLICT(failure_signature) DO UPDATE SET failure_count = excluded.failure_count, last_seen_at = excluded.last_seen_at, recovery_hint = excluded.recovery_hint")) {
                statement.setString(1, signature);
                statement.setInt(2, newValue);
                statement.setString(3, Instant.now().toString());
                statement.setString(4, recoveryHint == null ? "" : recoveryHint);
                statement.executeUpdate();
            } catch (SQLException ignored) {
                // best effort
            }
        }

        return newValue;
    }

    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(failureCounts));
    }
}
