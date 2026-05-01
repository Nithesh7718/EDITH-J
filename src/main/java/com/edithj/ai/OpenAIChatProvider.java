package com.edithj.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import com.edithj.ai.AiConfig.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class OpenAIChatProvider extends BaseHttpChatProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiConfig config;
    private final String apiKey;

    public OpenAIChatProvider(AiConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    OpenAIChatProvider(AiConfig config, HttpClient httpClient) {
        super("OpenAI", httpClient);
        this.config = config;
        this.apiKey = config.resolveApiKey(Provider.OPENAI);
    }

    @Override
    public String generateReply(String prompt) {
        String model = config.property("edith.ai.openai.model", "gpt-4o-mini");
        String body = toOpenAiRequestBody(model, prompt);

        String response = postJson(
                URI.create("https://api.openai.com/v1/chat/completions"),
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

    private boolean isDirectError(String value) {
        return value != null && (
                value.startsWith("Missing API key")
                || value.startsWith("Invalid provider endpoint")
                || value.startsWith("Unable to reach")
                || value.startsWith("OpenAI request")
                || value.contains("request failed with HTTP"));
    }
}
