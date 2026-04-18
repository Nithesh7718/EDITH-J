package com.edithj.assistant;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssistantStatusProbe {

    private static final Logger logger = LoggerFactory.getLogger(AssistantStatusProbe.class);
    private static final URI MODELS_URI = URI.create("https://api.groq.com/openai/v1/models");

    private final AssistantStatusService statusService;

    public AssistantStatusProbe() {
        this(AssistantStatusService.instance());
    }

    AssistantStatusProbe(AssistantStatusService statusService) {
        this.statusService = statusService;
    }

    public AssistantStatus runStartupProbe() {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            statusService.markOffline("Groq key missing");
            return AssistantStatus.OFFLINE;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(MODELS_URI)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                statusService.markOnline("AI ready");
                return AssistantStatus.ONLINE;
            }

            statusService.markOffline("Groq probe failed (HTTP " + response.statusCode() + ")");
            return AssistantStatus.OFFLINE;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.debug("Groq status probe interrupted", exception);
            statusService.markOffline("Groq probe interrupted");
            return AssistantStatus.OFFLINE;
        } catch (IOException exception) {
            logger.debug("Groq status probe IO failure", exception);
            statusService.markOffline("Groq unreachable");
            return AssistantStatus.OFFLINE;
        }
    }
}
