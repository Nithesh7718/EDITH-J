package com.edithj.integration.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.edithj.config.AppConfig;
import com.edithj.config.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GroqClient implements LlmClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ModelConfig modelConfig;
    private final HttpClient httpClient;

    public GroqClient() {
        this(AppConfig.load().modelConfig());
    }

    public GroqClient(ModelConfig modelConfig) {
        this.modelConfig = Objects.requireNonNull(modelConfig, "modelConfig");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(modelConfig.requestTimeout())
                .build();
    }

    @Override
    public String generateReply(String prompt) {
        String normalizedPrompt = prompt == null ? "" : prompt.trim();
        if (normalizedPrompt.isBlank()) {
            return "";
        }

        if (!modelConfig.isConfigured()) {
            return modelConfig.missingApiKeyMessage();
        }

        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "model", modelConfig.model(),
                    "messages", List.of(Map.of("role", "user", "content", normalizedPrompt)),
                    "temperature", modelConfig.temperature()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelConfig.chatCompletionsUrl()))
                    .timeout(modelConfig.requestTimeout())
                    .header("Authorization", "Bearer " + modelConfig.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "Groq request failed with HTTP " + response.statusCode();
            }

            return extractReply(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "Groq request was interrupted.";
        } catch (IOException exception) {
            return "Unable to reach Groq: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Unable to generate a reply right now.";
        }
    }

    private String extractReply(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "I could not generate a response right now.";
        }

        JsonNode content = choices.get(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            return "I could not generate a response right now.";
        }

        return content.asText().trim();
    }
}
