package com.edithj.integration.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.edithj.config.AppConfig;
import com.edithj.config.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GroqClient implements LlmClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> MODEL_PREFERENCES = List.of(
            "llama-3.3-70b-versatile",
            "llama3-70b-8192",
            "llama3-8b-8192",
            "llama-3.1-8b-instant");

    private final ModelConfig modelConfig;
    private final HttpClient httpClient;
    private final String resolvedModel;

    public GroqClient() {
        this(AppConfig.load().modelConfig());
    }

    public GroqClient(ModelConfig modelConfig) {
        this.modelConfig = Objects.requireNonNull(modelConfig, "modelConfig");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(modelConfig.requestTimeout())
                .build();
        this.resolvedModel = resolveModel();
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
                    "model", resolvedModel,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are EDITH-J, a helpful general-purpose desktop copilot. Provide accurate, practical help for coding, writing, planning, and everyday tasks. Be concise by default, ask clarifying questions when needed, and never claim actions were completed unless they were actually executed."),
                            Map.of("role", "user", "content", normalizedPrompt)),
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
                return formatHttpError(response.statusCode(), response.body());
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

    private String resolveModel() {
        String configuredModel = modelConfig.model();
        if (!modelConfig.isConfigured()) {
            return configuredModel;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelConfig.baseUrl() + "/models"))
                    .timeout(modelConfig.requestTimeout())
                    .header("Authorization", "Bearer " + modelConfig.apiKey())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return configuredModel;
            }

            Set<String> availableModels = parseAvailableModels(response.body());
            if (availableModels.contains(configuredModel)) {
                return configuredModel;
            }

            for (String preferredModel : MODEL_PREFERENCES) {
                if (availableModels.contains(preferredModel)) {
                    return preferredModel;
                }
            }

            return configuredModel;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return configuredModel;
        } catch (IOException | RuntimeException exception) {
            return configuredModel;
        }
    }

    private Set<String> parseAvailableModels(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode data = root.path("data");
        Set<String> models = new HashSet<>();

        if (data.isArray()) {
            for (JsonNode modelNode : data) {
                JsonNode idNode = modelNode.path("id");
                if (!idNode.isMissingNode() && !idNode.asText().isBlank()) {
                    models.add(idNode.asText().trim());
                }
            }
        }

        return models;
    }

    private String formatHttpError(int statusCode, String responseBody) {
        String message = extractErrorMessage(responseBody);
        if (message.isBlank()) {
            return "Groq request failed with HTTP " + statusCode;
        }

        return "Groq request failed with HTTP " + statusCode + ": " + message;
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                JsonNode message = error.path("message");
                if (!message.isMissingNode() && !message.asText().isBlank()) {
                    return message.asText().trim();
                }
            }

            String fallback = responseBody.replaceAll("\\s+", " ").trim();
            if (fallback.length() > 240) {
                return fallback.substring(0, 240) + "...";
            }

            return fallback;
        } catch (IOException exception) {
            String fallback = responseBody.replaceAll("\\s+", " ").trim();
            if (fallback.length() > 240) {
                return fallback.substring(0, 240) + "...";
            }

            return fallback;
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
