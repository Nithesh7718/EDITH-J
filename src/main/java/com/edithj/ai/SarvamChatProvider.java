package com.edithj.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import com.edithj.ai.AiConfig.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SarvamChatProvider extends BaseHttpChatProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiConfig config;
    private final String apiKey;

    public SarvamChatProvider(AiConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    SarvamChatProvider(AiConfig config, HttpClient httpClient) {
        super("Sarvam", httpClient);
        this.config = config;
        this.apiKey = config.resolveApiKey(Provider.SARVAM);
    }

    @Override
    public String generateReply(String prompt) {
        String baseUrl = config.property("edith.ai.sarvam.baseUrl", "https://api.sarvam.ai/v1");
        String model = config.property("edith.ai.sarvam.model", "sarvam-m");

        String body = toOpenAiRequestBody(model, prompt);
        String response = postJson(
                URI.create(trimTrailingSlash(baseUrl) + "/chat/completions"),
                apiKey,
                body,
                Map.of("Authorization", "Bearer " + apiKey));

        if (isDirectError(response)) {
            return response;
        }
        return extractOpenAiStyleContent(response);
    }

    private String toOpenAiRequestBody(String model, String prompt) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", safePrompt(prompt)))));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String safePrompt(String prompt) {
        return prompt == null ? "" : prompt;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://api.sarvam.ai/v1";
        }
        String out = value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private boolean isDirectError(String value) {
        return value != null && (
                value.startsWith("Missing API key")
                || value.startsWith("Invalid provider endpoint")
                || value.startsWith("Unable to reach")
                || value.startsWith("Sarvam request")
                || value.contains("request failed with HTTP"));
    }
}
