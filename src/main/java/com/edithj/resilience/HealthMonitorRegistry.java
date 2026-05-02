package com.edithj.resilience;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.edithj.storage.DatabaseManager;

public final class HealthMonitorRegistry {

    private static final HealthMonitorRegistry INSTANCE = new HealthMonitorRegistry();

    private final Map<String, ComponentHealthSnapshot> snapshots = new ConcurrentHashMap<>();
    private final List<Incident> incidents = Collections.synchronizedList(new ArrayList<>());
    private final AdaptiveFailureMemory failureMemory = new AdaptiveFailureMemory();
    private final RuntimeMetrics metrics = new RuntimeMetrics();
    private DatabaseManager databaseManager;
    private boolean initialized;

    private HealthMonitorRegistry() {
    }

    public static HealthMonitorRegistry instance() {
        return INSTANCE;
    }

    public synchronized void initialize(DatabaseManager databaseManager) {
        if (initialized) {
            return;
        }
        this.databaseManager = databaseManager;
        this.failureMemory.initialize(databaseManager);
        this.initialized = true;
    }

    public void registerHealthSignal(HealthSignal signal) {
        if (signal == null) {
            return;
        }
        ComponentHealthSnapshot snapshot = buildSnapshot(signal);
        snapshots.put(signal.componentId(), snapshot);

        if (signal.severity().ordinal() >= IncidentSeverity.MEDIUM.ordinal()) {
            Incident incident = new Incident(
                    null,
                    signal.componentId(),
                    signal.failureType(),
                    signal.severity(),
                    signal.message(),
                    new Diagnosis(signal.message(), signal.componentId(), "Investigate and recover the affected subsystem.", signal.metadata()),
                    signal.timestamp());
            logIncident(incident);
            if (signal.failureType() == FailureType.PROVIDER) {
                metrics.recordProviderFailoverAttempt();
            }
            if (signal.failureType() == FailureType.VOICE && signal.message().contains("fallback")) {
                metrics.recordDegradedModeEntry();
            }
        }
    }

    private ComponentHealthSnapshot buildSnapshot(HealthSignal signal) {
        String status = switch (signal.severity()) {
            case CRITICAL ->
                "critical";
            case HIGH ->
                "degraded";
            case MEDIUM ->
                "warning";
            default ->
                "healthy";
        };
        double score = Math.max(0.0, 1.0 - signal.severity().ordinal() * 0.25);
        String mode = signal.severity().ordinal() >= IncidentSeverity.HIGH.ordinal() ? "degraded" : "normal";
        String lastFailureAt = signal.severity().ordinal() >= IncidentSeverity.MEDIUM.ordinal() ? signal.timestamp().toString() : null;

        return new ComponentHealthSnapshot(
                signal.componentId(),
                status,
                score,
                mode,
                lastFailureAt,
                signal.timestamp().toString(),
                null);
    }

    private synchronized void logIncident(Incident incident) {
        incidents.add(incident);
        failureMemory.recordFailure(incident.componentId() + ":" + incident.failureType(), "Review repeated incident patterns for " + incident.componentId());
        if (databaseManager == null) {
            return;
        }

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO incidents(id, component_id, failure_type, severity, message, diagnosis, created_at) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, incident.id());
            statement.setString(2, incident.componentId());
            statement.setString(3, incident.failureType().name());
            statement.setString(4, incident.severity().name());
            statement.setString(5, incident.message());
            statement.setString(6, incident.diagnosis().recommendation());
            statement.setString(7, incident.createdAt().toString());
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // persistence is best-effort
        }
    }

    public synchronized void recordRecoveryAttempt(String incidentId, RecoveryAction action, boolean success) {
        if (incidentId == null || incidentId.isBlank() || action == null) {
            return;
        }

        if (success) {
            metrics.recordProviderFailoverSuccess();
            metrics.recordRecoveryLatency(100);
        }

        if (databaseManager == null) {
            return;
        }

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO recovery_actions(id, attempt_id, action_type, description, executed_at, result) VALUES(?, ?, ?, ?, ?, ?)")) {
            String actionId = java.util.UUID.randomUUID().toString();
            statement.setString(1, actionId);
            statement.setString(2, incidentId);
            statement.setString(3, action.actionType());
            statement.setString(4, action.description());
            statement.setString(5, Instant.now().toString());
            statement.setString(6, success ? "success" : "failure");
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // best effort
        }
    }

    public Map<String, Object> healthSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("snapshots", snapshotList());
        summary.put("recentIncidents", recentIncidents(20));
        summary.put("adaptiveFailureMemory", failureMemory.snapshot());
        summary.put("startupSuccessRate", metrics.startupSuccessRate());
        summary.put("meanTimeToDetectMillis", metrics.meanTimeToDetectMillis());
        summary.put("meanTimeToRecoverMillis", metrics.meanTimeToRecoverMillis());
        summary.put("providerFailoverSuccessRate", metrics.providerFailoverSuccessRate());
        summary.put("degradedModeContinuityCount", metrics.degradedModeContinuityCount());
        return summary;
    }

    public List<ComponentHealthSnapshot> snapshotList() {
        return snapshots.values().stream()
                .sorted(Comparator.comparing(ComponentHealthSnapshot::componentId))
                .toList();
    }

    public List<Incident> recentIncidents(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        synchronized (incidents) {
            int size = incidents.size();
            int fromIndex = Math.max(0, size - limit);
            return new ArrayList<>(incidents.subList(fromIndex, size));
        }
    }

    public void markStartupSuccess() {
        metrics.recordStartupSuccess();
    }

    public void markStartupFailure() {
        metrics.recordStartupFailure();
    }
}
