package com.edithj.commands;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.assistant.IntentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WeatherCommandHandler implements CommandHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "(?:weather|forecast|temperature|rain)(?:\\s+(?:in|for|at|of))?\\s+([a-zA-Z .'-]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FALLBACK_LOCATION_PATTERN = Pattern.compile(
            "(?:in|for|at)\\s+([a-zA-Z .'-]+?)(?:\\s+(?:today|now))?$",
            Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;

    public WeatherCommandHandler() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
    }

    public WeatherCommandHandler(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public IntentType intentType() {
        return IntentType.WEATHER;
    }

    @Override
    public String handle(CommandContext context) {
        String input = context == null ? "" : context.normalizedInput();
        String location = extractLocation(input);
        if (location.isBlank()) {
            return "Tell me the place, for example: weather in Pollachi today.";
        }

        try {
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://wttr.in/" + encodedLocation + "?format=j1"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "I could not fetch live weather right now. Please try again in a moment.";
            }

            return summarize(location, response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "Weather check was interrupted. Please try again.";
        } catch (IOException exception) {
            return "I could not fetch live weather right now. Please check your network and try again.";
        }
    }

    private String summarize(String location, String body) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(body);

        JsonNode current = root.path("current_condition").isArray() && root.path("current_condition").size() > 0
                ? root.path("current_condition").get(0)
                : null;
        JsonNode today = root.path("weather").isArray() && root.path("weather").size() > 0
                ? root.path("weather").get(0)
                : null;

        if (current == null || today == null) {
            return "I found no weather data for " + location + " right now.";
        }

        String condition = extractCondition(current);
        String tempC = current.path("temp_C").asText("-");
        String feelsLikeC = current.path("FeelsLikeC").asText("-");
        String minC = today.path("mintempC").asText("-");
        String maxC = today.path("maxtempC").asText("-");
        String humidity = current.path("humidity").asText("-");

        return String.format(
                Locale.ROOT,
                "Today's forecast for %s: %s. Now %s C (feels like %s C), low/high %s/%s C, humidity %s%%.",
                toTitleCase(location),
                condition,
                tempC,
                feelsLikeC,
                minC,
                maxC,
                humidity);
    }

    private String extractCondition(JsonNode current) {
        JsonNode weatherDesc = current.path("weatherDesc");
        if (weatherDesc.isArray() && weatherDesc.size() > 0) {
            String value = weatherDesc.get(0).path("value").asText("");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "No condition available";
    }

    private String extractLocation(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String cleaned = input.trim().replaceAll("\\s+", " ");

        Matcher matcher = LOCATION_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return normalizeLocation(matcher.group(1));
        }

        Matcher fallbackMatcher = FALLBACK_LOCATION_PATTERN.matcher(cleaned);
        if (fallbackMatcher.find()) {
            return normalizeLocation(fallbackMatcher.group(1));
        }

        return "";
    }

    private String normalizeLocation(String location) {
        if (location == null) {
            return "";
        }

        String normalized = location
                .replaceAll("(?i)^(the|city of)\\s+", "")
                .replaceAll("(?i)\\s+(today|now|please)$", "")
                .trim();
        return normalized;
    }

    private String toTitleCase(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }
}
