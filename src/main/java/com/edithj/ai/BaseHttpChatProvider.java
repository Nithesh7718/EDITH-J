package com.edithj.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

abstract class BaseHttpChatProvider implements ChatProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String providerName;
    private final HttpClient httpClient;

    BaseHttpChatProvider(String providerName, HttpClient httpClient) {
        this.providerName = Objects.requireNonNull(providerName, "providerName");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public String providerName() {
        return providerName;
    }

    protected HttpClient httpClient() {
        return httpClient;
    }

    protected String postJson(URI uri, String apiKey, String requestBody, Map<String, String> headers) {
        if (uri == null) {
            return "Invalid provider endpoint configuration.";
        }
        if (apiKey == null || apiKey.isBlank()) {
            return "Missing API key for " + providerName + ". Set the environment variable first, or use edith.properties override.";
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

        if (headers != null) {
            headers.forEach(builder::header);
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return providerName + " request failed with HTTP " + response.statusCode() + ".";
            }
            return response.body() == null ? "" : response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return providerName + " request was interrupted.";
        } catch (IOException exception) {
            return "Unable to reach " + providerName + " right now.";
        }
    }

    protected String extractOpenAiStyleContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "I could not generate a response right now.";
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return "I could not generate a response right now.";
            }
            String content = choices.get(0).path("message").path("content").asText("").trim();
            return content.isBlank() ? "I could not generate a response right now." : content;
        } catch (IOException exception) {
            return "I could not parse the model response.";
        }
    }

    protected String extractGeminiContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "I could not generate a response right now.";
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return "I could not generate a response right now.";
            }
            String content = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("")
                    .trim();
            return content.isBlank() ? "I could not generate a response right now." : content;
        } catch (IOException exception) {
            return "I could not parse the model response.";
        }
    }
}
