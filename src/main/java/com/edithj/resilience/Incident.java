package com.edithj.resilience;

import java.time.Instant;
import java.util.UUID;

public record Incident(
        String id,
        String componentId,
        FailureType failureType,
        IncidentSeverity severity,
        String message,
        Diagnosis diagnosis,
        Instant createdAt) {

    public Incident {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (componentId == null || componentId.isBlank()) {
            throw new IllegalArgumentException("componentId cannot be blank");
        }
        if (failureType == null) {
            failureType = FailureType.UNKNOWN;
        }
        if (severity == null) {
            severity = IncidentSeverity.MEDIUM;
        }
        if (message == null) {
            message = "";
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
