package com.edithj.ai;

import java.util.Objects;

public final class MissingApiKeyChatProvider implements ChatProvider {

    private final String providerName;
    private final String message;

    public MissingApiKeyChatProvider(String providerName, String message) {
        this.providerName = providerName == null ? "unknown" : providerName.trim().toLowerCase();
        this.message = Objects.requireNonNullElse(message,
                "AI provider is not configured. Set an API key in environment variables or edith.properties.");
    }

    @Override
    public String providerName() {
        return providerName;
    }

    @Override
    public String generateReply(String prompt) {
        return message;
    }
}
