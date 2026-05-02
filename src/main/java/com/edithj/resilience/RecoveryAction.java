package com.edithj.resilience;

import java.util.Objects;
import java.util.function.Supplier;

public record RecoveryAction(
        String id,
        String actionType,
        String description,
        Supplier<Boolean> executor) {

    public RecoveryAction {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (actionType == null || actionType.isBlank()) {
            actionType = "UNKNOWN";
        }
        if (description == null) {
            description = "";
        }
        if (executor == null) {
            executor = () -> false;
        }
    }

    public boolean execute() {
        try {
            return Objects.requireNonNull(executor).get();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
