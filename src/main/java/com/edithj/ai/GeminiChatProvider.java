package com.edithj.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.edithj.ai.AiConfig.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class GeminiChatProvider extends BaseHttpChatProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiConfig config;
    private final String apiKey;

    public GeminiChatProvider(AiConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    GeminiChatProvider(AiConfig config, HttpClient httpClient) {
        super("Gemini", httpClient);
        this.config = config;
        this.apiKey = config.resolveApiKey(Provider.GEMINI);
    }

    @Override
    public String generateReply(String prompt) {
        String model = config.property("edith.ai.gemini.model", "gemini-1.5-flash");
        String encodedKey = URLEncoder.encode(apiKey == null ? "" : apiKey, StandardCharsets.UTF_8);
        URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                + model
                + ":generateContent?key="
                + encodedKey);

        String body = toGeminiRequestBody(prompt);
        String response = postJson(uri, apiKey, body, Map.of());
        if (isDirectError(response)) {
            return response;
        }
        return extractGeminiContent(response);
    }

    private String toGeminiRequestBody(String prompt) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", safePrompt(prompt)))))));
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
                || value.startsWith("Gemini request")
                || value.contains("request failed with HTTP"));
    }
}
