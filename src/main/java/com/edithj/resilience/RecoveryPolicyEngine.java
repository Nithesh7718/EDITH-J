package com.edithj.resilience;

import java.util.ArrayList;
import java.util.List;

public final class RecoveryPolicyEngine {

    public RecoveryPlan planRecoveryFor(Incident incident, ComponentHealthSnapshot snapshot) {
        List<RecoveryAction> actions = new ArrayList<>();
        String component = incident.componentId();

        switch (incident.failureType()) {
            case PROVIDER ->
                actions.add(providerFailoverAction(component, incident));
            case STORAGE ->
                actions.add(reconnectStorageAction(component, incident));
            case VOICE ->
                actions.add(validateAudioPipelineAction(component, incident));
            case STARTUP, FRONTEND ->
                actions.add(validatePackageResourceAction(component, incident));
            case AUTOMATION ->
                actions.add(fallbackAutomationAction(component, incident));
            default ->
                actions.add(genericRecoveryAction(component, incident));
        }

        if (snapshot != null && snapshot.healthScore() < 0.5d) {
            actions.add(new RecoveryAction(
                    "enter-degraded-mode",
                    "ENTER_DEGRADED_MODE",
                    "Move the component into a bounded degraded mode and preserve user continuity.",
                    () -> true));
        }

        return new RecoveryPlan(null, incident, List.copyOf(actions), null);
    }

    private RecoveryAction providerFailoverAction(String component, Incident incident) {
        return new RecoveryAction(
                "provider-failover",
                "SWITCH_PROVIDER",
                "Attempt a provider failover to a healthier API endpoint for " + component + ". Reason: " + incident.message(),
                () -> true);
    }

    private RecoveryAction reconnectStorageAction(String component, Incident incident) {
        return new RecoveryAction(
                "storage-reconnect",
                "RETRY_CONNECTION",
                "Retry the local SQLite connection for " + component + " after: " + incident.message(),
                () -> true);
    }

    private RecoveryAction validateAudioPipelineAction(String component, Incident incident) {
        return new RecoveryAction(
                "validate-audio-pipeline",
                "VALIDATE_VOICE_PIPELINE",
                "Verify the packaged speech model and fall back to typed input for " + component + " if required. Reason: " + incident.message(),
                () -> true);
    }

    private RecoveryAction validatePackageResourceAction(String component, Incident incident) {
        return new RecoveryAction(
                "validate-packaged-resources",
                "VALIDATE_ASSETS",
                "Confirm that packaged frontend and model assets for " + component + " are present and readable. Reason: " + incident.message(),
                () -> true);
    }

    private RecoveryAction fallbackAutomationAction(String component, Incident incident) {
        return new RecoveryAction(
                "automation-degraded-mode",
                "ENTER_AUTOMATION_DEGRADED_MODE",
                "Limit automation to safe local actions for " + component + " while preserving session continuity. Reason: " + incident.message(),
                () -> true);
    }

    private RecoveryAction genericRecoveryAction(String component, Incident incident) {
        return new RecoveryAction(
                "generic-recovery",
                "RETRY_OPERATION",
                "Retry the last operation for " + component + " and collect more diagnostic data. Reason: " + incident.message(),
                () -> true);
    }
}
