package com.edithj.assistant;

import java.util.List;
import java.util.Locale;

public class ActionApprovalPolicy {

    public ApprovalDecision evaluate(String normalizedInput, IntentType intentType) {
        String input = normalizedInput == null ? "" : normalizedInput.trim();
        if (input.isBlank()) {
            return ApprovalDecision.safeAuto("No action requested.");
        }

        if (intentType != IntentType.DESKTOP_AUTOMATION) {
            return ApprovalDecision.safeAuto("Action is safe to run automatically.");
        }

        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.startsWith("file rename ")) {
            return ApprovalDecision.confirmOnce(
                    "file_rename",
                    "Renaming a file changes the workspace state. I want your approval before proceeding.");
        }
        if (lower.startsWith("file move ")) {
            return ApprovalDecision.confirmOnce(
                    "file_move",
                    "Moving a file changes the workspace state. I want your approval before proceeding.");
        }
        if (lower.startsWith("write code ")) {
            return ApprovalDecision.confirmOnce(
                    "write_code",
                    "This will generate and save a code file. I want your approval before writing to disk.");
        }
        if (lower.startsWith("write document ")) {
            return ApprovalDecision.confirmOnce(
                    "write_document",
                    "This will generate and save a document. I want your approval before writing to disk.");
        }
        if (lower.startsWith("install ") || lower.contains(" install app ")) {
            return ApprovalDecision.blocked(
                    "app_install",
                    "App installation is blocked until EDITH-J has an explicit installer workflow.");
        }
        if (lower.startsWith("delete ") || lower.startsWith("file delete ") || lower.contains(" remove file ")) {
            return ApprovalDecision.blocked(
                    "file_delete",
                    "File deletion is blocked until EDITH-J has a dedicated destructive-action workflow.");
        }

        return ApprovalDecision.safeAuto("Action is safe to run automatically.");
    }

    public record ApprovalDecision(
            ExecutionMode mode,
            String approvalType,
            String explanation,
            List<AssistantResponse.AssistantAction> actions,
            List<AssistantResponse.RecoveryOption> recoveryOptions) {

        public ApprovalDecision {
            mode = mode == null ? ExecutionMode.SAFE_AUTO : mode;
            approvalType = approvalType == null ? "" : approvalType;
            explanation = explanation == null ? "" : explanation;
            actions = actions == null ? List.of() : List.copyOf(actions);
            recoveryOptions = recoveryOptions == null ? List.of() : List.copyOf(recoveryOptions);
        }

        public static ApprovalDecision safeAuto(String explanation) {
            return new ApprovalDecision(ExecutionMode.SAFE_AUTO, "", explanation, List.of(), List.of());
        }

        public static ApprovalDecision confirmOnce(String approvalType, String explanation) {
            return new ApprovalDecision(
                    ExecutionMode.CONFIRM_ONCE,
                    approvalType,
                    explanation,
                    List.of(
                            new AssistantResponse.AssistantAction("approve-last-action", "Approve", "approval", "approve last action"),
                            new AssistantResponse.AssistantAction("cancel-last-action", "Cancel", "approval", "cancel last action")),
                    List.of());
        }

        public static ApprovalDecision blocked(String approvalType, String explanation) {
            return new ApprovalDecision(
                    ExecutionMode.BLOCKED,
                    approvalType,
                    explanation,
                    List.of(),
                    List.of(new AssistantResponse.RecoveryOption("Try a safer action", "show me safer alternatives for this task")));
        }
    }

    public enum ExecutionMode {
        SAFE_AUTO,
        CONFIRM_ONCE,
        BLOCKED
    }
}
