package com.edithj.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 * Connectivity probe to compare Java HttpClient behavior with curl against the
 * Groq models endpoint from the current environment.
 */
class GroqConnectivityProbeTest {

    @Test
    void javaHttpClient_canReachGroqModelsEndpoint() {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            fail("GROQ_API_KEY is missing or blank");
            return;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.groq.com/openai/v1/models"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body() == null ? "" : response.body();
            String snippet = body.length() <= 200 ? body : body.substring(0, 200);

            System.out.println("Groq connectivity probe status: " + response.statusCode());
            System.out.println("Groq connectivity probe body (first 200 chars): " + snippet);

            assertEquals(200, response.statusCode(), "Expected HTTP 200 from Groq models endpoint");
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            fail("Groq connectivity probe failed with "
                    + exception.getClass().getSimpleName()
                    + ": "
                    + exception.getMessage());
        }
    }
}
