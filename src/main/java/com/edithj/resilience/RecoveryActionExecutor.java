package com.edithj.resilience;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RecoveryActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryActionExecutor.class);
    private final HealthMonitorRegistry registry;

    public RecoveryActionExecutor(HealthMonitorRegistry registry) {
        this.registry = registry;
    }

    public boolean executePlan(RecoveryPlan plan) {
        if (plan == null) {
            return false;
        }

        boolean allSuccessful = true;
        List<RecoveryAction> actions = plan.actions();
        for (RecoveryAction action : actions) {
            boolean result = action.execute();
            registry.recordRecoveryAttempt(plan.incident().id(), action, result);
            if (!result) {
                allSuccessful = false;
                logger.warn("Recovery action failed: {} - {}", action.actionType(), action.description());
            }
        }
        return allSuccessful;
    }
}
