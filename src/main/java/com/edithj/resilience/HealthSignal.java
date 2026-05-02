package com.edithj.resilience;

import java.time.Instant;
import java.util.Map;

public record HealthSignal(
        String componentId,
        FailureType failureType,
        IncidentSeverity severity,
        String message,
        Instant timestamp,
        Map<String, String> metadata) {

    public HealthSignal {
        if (componentId == null || componentId.isBlank()) {
            throw new IllegalArgumentException("componentId cannot be blank");
        }
        if (failureType == null) {
            failureType = FailureType.UNKNOWN;
        }
        if (severity == null) {
            severity = IncidentSeverity.LOW;
        }
        if (message == null) {
            message = "";
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
