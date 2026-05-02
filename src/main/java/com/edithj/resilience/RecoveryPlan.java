package com.edithj.resilience;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecoveryPlan(
        String id,
        Incident incident,
        List<RecoveryAction> actions,
        Instant createdAt) {

    public RecoveryPlan {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (incident == null) {
            throw new IllegalArgumentException("incident cannot be null");
        }
        if (actions == null) {
            actions = List.of();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
