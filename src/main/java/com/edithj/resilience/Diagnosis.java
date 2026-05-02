package com.edithj.resilience;

import java.util.Map;

public record Diagnosis(
        String rootCause,
        String componentId,
        String recommendation,
        Map<String, String> evidence) {

    public Diagnosis {
        if (componentId == null || componentId.isBlank()) {
            throw new IllegalArgumentException("componentId cannot be blank");
        }
        if (rootCause == null) {
            rootCause = "unspecified";
        }
        if (recommendation == null) {
            recommendation = "review incident details";
        }
        if (evidence == null) {
            evidence = Map.of();
        }
    }
}
