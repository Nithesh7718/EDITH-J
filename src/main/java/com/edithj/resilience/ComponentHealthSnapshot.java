package com.edithj.resilience;

public record ComponentHealthSnapshot(
        String componentId,
        String status,
        double healthScore,
        String healthMode,
        String lastFailureAt,
        String lastSuccessAt,
        String currentIncidentId) {

    public ComponentHealthSnapshot {
        if (componentId == null || componentId.isBlank()) {
            throw new IllegalArgumentException("componentId cannot be blank");
        }
        if (status == null || status.isBlank()) {
            status = "unknown";
        }
        if (healthMode == null || healthMode.isBlank()) {
            healthMode = "normal";
        }
    }
}
