package com.edithj.chat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.edithj.assistant.AssistantResponse;

public record ChatMessage(
        String id,
        String role,
        String content,
        Instant timestamp,
        String source,
        String intentType,
        boolean success,
        boolean requiresApproval,
        String approvalType,
        String explanation,
        String planGoal,
        AssistantResponse.TaskPlan taskPlan,
        List<AssistantResponse.AssistantAction> actions,
        List<AssistantResponse.RecoveryOption> recoveryOptions,
        Map<String, String> metadata) {
    public ChatMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        role = role == null || role.isBlank() ? "edith" : role;
        content = content == null ? "" : content;
        timestamp = timestamp == null ? Instant.now() : timestamp;
        source = source == null || source.isBlank() ? defaultSource(role) : source;
        intentType = intentType == null ? "" : intentType;
        approvalType = approvalType == null ? "" : approvalType;
        explanation = explanation == null ? "" : explanation;
        planGoal = planGoal == null ? "" : planGoal;
        taskPlan = taskPlan == null
                ? (planGoal.isBlank() ? null : new AssistantResponse.TaskPlan(planGoal, List.of()))
                : taskPlan;
        actions = actions == null ? List.of() : List.copyOf(actions);
        recoveryOptions = recoveryOptions == null ? List.of() : List.copyOf(recoveryOptions);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public ChatMessage(String role, String content) {
        this(UUID.randomUUID().toString(), role, content, Instant.now(), defaultSource(role), "", true, false, "", "", "",
                null, List.of(), List.of(), Map.of());
    }

    public ChatMessage(String role, String content, String source, String intentType, boolean success) {
        this(UUID.randomUUID().toString(), role, content, Instant.now(), source, intentType, success, false, "", "", "",
                null, List.of(), List.of(), Map.of());
    }

    public ChatMessage(String role, String content, String source, String intentType, boolean success,
            boolean requiresApproval, String approvalType, String explanation) {
        this(UUID.randomUUID().toString(), role, content, Instant.now(), source, intentType, success,
                requiresApproval, approvalType, explanation, "", null, List.of(), List.of(), Map.of());
    }

    public ChatMessage(String role, String content, String source, String intentType, boolean success,
            boolean requiresApproval, String approvalType, String explanation, String planGoal) {
        this(UUID.randomUUID().toString(), role, content, Instant.now(), source, intentType, success,
                requiresApproval, approvalType, explanation, planGoal, null, List.of(), List.of(), Map.of());
    }

    public ChatMessage(String role, String content, String source, String intentType, boolean success,
            boolean requiresApproval, String approvalType, String explanation, String planGoal,
            AssistantResponse.TaskPlan taskPlan, List<AssistantResponse.AssistantAction> actions,
            List<AssistantResponse.RecoveryOption> recoveryOptions, Map<String, String> metadata) {
        this(UUID.randomUUID().toString(), role, content, Instant.now(), source, intentType, success,
                requiresApproval, approvalType, explanation, planGoal, taskPlan, actions, recoveryOptions, metadata);
    }

    private static String defaultSource(String role) {
        return "user".equalsIgnoreCase(role) ? "User" : "AI";
    }
}
