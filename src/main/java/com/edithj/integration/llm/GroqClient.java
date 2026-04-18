package com.edithj.integration.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.config.AppConfig;
import com.edithj.config.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GroqClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(GroqClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final URI CHAT_COMPLETIONS_URI = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
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
        clearProxySystemProperties();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.resolvedModel = resolveModel();
    }

    @Override
    public String generateReply(String prompt) {
        String normalizedPrompt = prompt == null ? "" : prompt.trim();
        if (normalizedPrompt.isBlank()) {
            logger.debug("Empty prompt, returning empty reply");
            return "";
        }

        if (!modelConfig.isConfigured()) {
            logger.warn("Groq is not configured - GROQ_API_KEY not set");
            return modelConfig.missingApiKeyMessage();
        }

        long startNanos = System.nanoTime();
        String apiKey = modelConfig.apiKey();
        try {
            logger.debug("Generating reply with model: {}", resolvedModel);
            String requestBody = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "model", resolvedModel,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are EDITH-J, a helpful general-purpose desktop copilot. Provide accurate, practical help for coding, writing, planning, and everyday tasks. Be concise by default, ask clarifying questions when needed, and never claim actions were completed unless they were actually executed."),
                            Map.of("role", "user", "content", normalizedPrompt)),
                    "temperature", modelConfig.temperature()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(CHAT_COMPLETIONS_URI)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            logger.info("Calling Groq URI={} connectTimeout={} requestTimeout={} apiKeyPrefix={}...",
                    CHAT_COMPLETIONS_URI,
                    CONNECT_TIMEOUT,
                    REQUEST_TIMEOUT,
                    maskApiKeyPrefix(apiKey));
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            logger.debug("Groq request completed in {} ms with HTTP {}", elapsedMs, response.statusCode());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.error("Groq API error - HTTP {}: {}", response.statusCode(), response.body());
                return formatHttpError(response.statusCode(), response.body());
            }

            logger.debug("Groq API returned HTTP {}", response.statusCode());
            return extractReply(response.body());
        } catch (InterruptedException exception) {
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            logger.warn("Groq request failed after {} ms - type={} message={}",
                    elapsedMs,
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            Thread.currentThread().interrupt();
            return "Groq request was interrupted.";
        } catch (IOException exception) {
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            logger.error("Groq request failed after {} ms - type={} message={}",
                    elapsedMs,
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            return "Unable to reach Groq: " + exception.getMessage();
        } catch (RuntimeException exception) {
            logger.error("Unexpected error while generating reply", exception);
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

    private void clearProxySystemProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
    }

    private String maskApiKeyPrefix(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "missing";
        }

        String trimmed = apiKey.trim();
        int previewLength = Math.min(6, trimmed.length());
        return trimmed.substring(0, previewLength);
    }
}
