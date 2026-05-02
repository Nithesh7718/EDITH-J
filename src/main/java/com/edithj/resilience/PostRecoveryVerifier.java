package com.edithj.resilience;

public final class PostRecoveryVerifier {

    public boolean verify(RecoveryPlan plan) {
        if (plan == null || plan.actions().isEmpty()) {
            return false;
        }

        return plan.actions().stream()
                .allMatch(RecoveryAction::execute);
    }
}
