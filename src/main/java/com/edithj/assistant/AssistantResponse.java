package com.edithj.assistant;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AssistantResponse(
        IntentType intentType,
        String userInput,
        String answer,
        String channel,
        String source,
        boolean success,
        boolean requiresApproval,
        String approvalType,
        String explanation,
        List<AssistantAction> actions,
        List<RecoveryOption> recoveryOptions,
        Map<String, String> metadata) {

    public AssistantResponse {
        intentType = Objects.requireNonNull(intentType, "intentType");
        userInput = userInput == null ? "" : userInput;
        answer = answer == null ? "" : answer;
        channel = channel == null || channel.isBlank() ? "typed" : channel;
        source = source == null || source.isBlank() ? defaultSource(intentType) : source;
        approvalType = approvalType == null ? "" : approvalType;
        explanation = explanation == null ? "" : explanation;
        actions = actions == null ? List.of() : List.copyOf(actions);
        recoveryOptions = recoveryOptions == null ? List.of() : List.copyOf(recoveryOptions);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public AssistantResponse(IntentType intentType, String userInput, String answer, String channel) {
        this(intentType, userInput, answer, channel, defaultSource(intentType), true, false, "",
                "", List.of(), List.of(), Map.of());
    }

    public static String defaultSource(IntentType intentType) {
        if (intentType == null) {
            return "AI";
        }
        return switch (intentType) {
            case NOTES -> "Notes";
            case REMINDERS -> "Reminders";
            case APP_LAUNCH -> "Launcher";
            case EMAIL, CALENDAR, DESKTOP_TOOLS, DESKTOP_AUTOMATION, WHATSAPP -> "Automation";
            case WEATHER, UTILITIES -> "Tools";
            case ASK_WEB -> "Web";
            case ASK_LOCAL_KB -> "Knowledge";
            case ASK_WORLD, ASK_WORLD_MARKETS, ASK_WORLD_RISK -> "World";
            case GENERAL_CHAT, FALLBACK_CHAT -> "AI";
        };
    }

    public record AssistantAction(String id, String label, String kind, String value) {
        public AssistantAction {
            id = id == null ? "" : id;
            label = label == null ? "" : label;
            kind = kind == null ? "" : kind;
            value = value == null ? "" : value;
        }
    }

    public record RecoveryOption(String label, String prompt) {
        public RecoveryOption {
            label = label == null ? "" : label;
            prompt = prompt == null ? "" : prompt;
        }
    }
}
