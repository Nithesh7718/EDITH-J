package com.edithj.assistant;

import java.util.Objects;

public class AssistantResponse {

    private final IntentType intentType;
    private final String userInput;
    private final String answer;
    private final String channel;

    public AssistantResponse(IntentType intentType, String userInput, String answer, String channel) {
        this.intentType = Objects.requireNonNull(intentType, "intentType");
        this.userInput = userInput == null ? "" : userInput;
        this.answer = answer == null ? "" : answer;
        this.channel = channel == null ? "typed" : channel;
    }

    public IntentType intentType() {
        return intentType;
    }

    public String userInput() {
        return userInput;
    }

    public String answer() {
        return answer;
    }

    public String channel() {
        return channel;
    }
}
