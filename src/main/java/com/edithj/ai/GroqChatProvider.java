package com.edithj.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import com.edithj.ai.AiConfig.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class GroqChatProvider extends BaseHttpChatProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiConfig config;
    private final String apiKey;

    public GroqChatProvider(AiConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    GroqChatProvider(AiConfig config, HttpClient httpClient) {
        super("Groq", httpClient);
        this.config = config;
        this.apiKey = config.resolveApiKey(Provider.GROQ);
    }

    @Override
    public String generateReply(String prompt) {
        String baseUrl = config.property("groq.base-url", "https://api.groq.com/openai/v1");
        String model = config.property("groq.model", "llama-3.3-70b-versatile");
        double temperature = parseTemperature(config.property("groq.temperature", "0.2"));

        String body = toOpenAiRequestBody(model, prompt, temperature);
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

    private String toOpenAiRequestBody(String model, String prompt, double temperature) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", safePrompt(prompt))),
                    "temperature", temperature));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String safePrompt(String prompt) {
        return prompt == null ? "" : prompt;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://api.groq.com/openai/v1";
        }
        String out = value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private double parseTemperature(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (RuntimeException exception) {
            return 0.2d;
        }
    }

    private boolean isDirectError(String value) {
        return value != null && (
                value.startsWith("Missing API key")
                || value.startsWith("Invalid provider endpoint")
                || value.startsWith("Unable to reach")
                || value.startsWith("Groq request")
                || value.contains("request failed with HTTP"));
    }
}
