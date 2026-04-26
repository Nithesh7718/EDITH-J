package com.edithj.integration.worldmonitor;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.config.AppConfig;
import com.edithj.config.EnvConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Dedicated World Monitor hub client for conflicts, instability, and market
 * signals.
 */
public class WorldMonitorClient {

    private static final Logger logger = LoggerFactory.getLogger(WorldMonitorClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(4);

    private static final String DEFAULT_BASE_URL = "https://www.worldmonitor.app";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public WorldMonitorClient() {
        this(AppConfig.load().envConfig(), AppConfig.load().properties());
    }

    WorldMonitorClient(EnvConfig envConfig, java.util.Properties properties) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();

        String configuredBaseUrl = envConfig.get("WORLD_MONITOR_BASE_URL")
                .orElseGet(() -> properties.getProperty("worldmonitor.base-url", DEFAULT_BASE_URL));
        this.baseUrl = stripTrailingSlash(configuredBaseUrl);

        this.apiKey = envConfig.get("WORLD_MONITOR_API_KEY")
                .orElseGet(() -> properties.getProperty("worldmonitor.api-key", ""));
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public List<ConflictEvent> getRecentConflicts(String region, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 30));
        String safeRegion = normalizeRegion(region);
        String cacheKey = "conflicts:" + safeRegion + ":" + boundedLimit;

        JsonNode data = getOrFetch(cacheKey, () -> fetchJson("/api/conflict/v1/events", Map.of(
                "region", safeRegion,
                "limit", String.valueOf(boundedLimit)
        )));

        List<ConflictEvent> events = new ArrayList<>();
        JsonNode items = selectArrayNode(data, "events", "items", "data", "results");
        if (items != null) {
            for (JsonNode item : items) {
                events.add(new ConflictEvent(
                        text(item, "id", "eventId", "uuid"),
                        text(item, "title", "headline", "summary"),
                        text(item, "region", "country", "location"),
                        text(item, "severity", "riskLevel", "priority"),
                        parseInstant(text(item, "timestamp", "publishedAt", "createdAt"))));
            }
        }

        events.sort(Comparator.comparing(ConflictEvent::timestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        if (events.size() > boundedLimit) {
            return events.subList(0, boundedLimit);
        }
        return events;
    }

    public CountryInstability getCountryInstability(String isoCode) {
        String code = (isoCode == null || isoCode.isBlank()) ? "GLOBAL" : isoCode.trim().toUpperCase(Locale.ROOT);
        JsonNode data = getOrFetch("instability:" + code, () -> fetchJson("/api/intel/v1/cii", Map.of("isoCode", code)));

        double score = number(data, "score", "instabilityScore", "cii", "value");
        String level = text(data, "level", "riskLevel", "band");
        String summary = text(data, "summary", "description", "insight");

        return new CountryInstability(code, score, level, summary);
    }

    public MarketSnapshot getMarketSnapshot(List<String> symbols) {
        List<String> safeSymbols = (symbols == null || symbols.isEmpty())
                ? List.of("SPY", "QQQ", "BTC-USD")
                : symbols.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).toList();

        String joined = String.join(",", safeSymbols);
        JsonNode data = getOrFetch("markets:" + joined, () -> fetchJson("/api/markets/v1/quotes", Map.of("symbols", joined)));

        List<MarketQuote> quotes = new ArrayList<>();
        JsonNode items = selectArrayNode(data, "quotes", "items", "data", "results");
        if (items != null) {
            for (JsonNode item : items) {
                double price = number(item, "price", "last", "value");
                double changePct = number(item, "changePercent", "pct", "percentChange");
                String trend = changePct > 0.05 ? "up" : changePct < -0.05 ? "down" : "flat";
                quotes.add(new MarketQuote(
                        text(item, "symbol", "ticker", "id"),
                        price,
                        changePct,
                        trend));
            }
        }

        return new MarketSnapshot(Instant.now(), quotes);
    }

    public WorldSnapshot getWorldSnapshot(String region, String isoCode, List<String> symbols) {
        List<ConflictEvent> conflicts = getRecentConflicts(region, 8);
        CountryInstability instability = getCountryInstability(isoCode);
        MarketSnapshot marketSnapshot = getMarketSnapshot(symbols);
        return new WorldSnapshot(Instant.now(), conflicts, instability, marketSnapshot);
    }

    public Instant lastCacheUpdate() {
        return cache.values().stream()
                .map(CacheEntry::fetchedAt)
                .max(Instant::compareTo)
                .orElse(null);
    }

    private JsonNode getOrFetch(String cacheKey, JsonSupplier supplier) {
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.payload();
        }

        JsonNode payload = supplier.get();
        cache.put(cacheKey, new CacheEntry(payload, Instant.now().plus(DEFAULT_TTL), Instant.now()));
        return payload;
    }

    private JsonNode fetchJson(String path, Map<String, String> query) {
        if (!isConfigured()) {
            throw new IllegalStateException("World Monitor API key is not configured. Set WORLD_MONITOR_API_KEY.");
        }

        URI uri = buildUri(path, query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-API-Key", apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new IllegalStateException("World Monitor rate limit reached. Please retry shortly.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = response.body() == null ? "" : response.body();
                throw new IllegalStateException("World Monitor request failed with HTTP " + response.statusCode() + ": " + truncate(body));
            }

            return OBJECT_MAPPER.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("World Monitor request was interrupted.", exception);
        } catch (IOException exception) {
            logger.warn("World Monitor request failed: {}", exception.getMessage());
            throw new IllegalStateException("Unable to reach World Monitor: " + exception.getMessage(), exception);
        }
    }

    private URI buildUri(String path, Map<String, String> query) {
        StringBuilder builder = new StringBuilder(baseUrl).append(path);
        if (query != null && !query.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                builder.append(first ? '?' : '&');
                first = false;
                builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                builder.append('=');
                builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        return URI.create(builder.toString());
    }

    private JsonNode selectArrayNode(JsonNode root, String... fields) {
        if (root == null || root.isMissingNode()) {
            return null;
        }

        for (String field : fields) {
            JsonNode candidate = root.path(field);
            if (candidate.isArray()) {
                return candidate;
            }
        }

        if (root.isArray()) {
            return root;
        }
        return null;
    }

    private String text(JsonNode node, String... fields) {
        if (node == null) {
            return "";
        }

        for (String field : fields) {
            JsonNode candidate = node.path(field);
            if (!candidate.isMissingNode() && !candidate.isNull()) {
                String value = candidate.asText("").trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }

        return "";
    }

    private double number(JsonNode node, String... fields) {
        if (node == null) {
            return 0.0d;
        }

        for (String field : fields) {
            JsonNode candidate = node.path(field);
            if (candidate.isNumber()) {
                return candidate.asDouble();
            }
            String asText = candidate.asText("").trim();
            if (!asText.isBlank()) {
                try {
                    return Double.parseDouble(asText);
                } catch (NumberFormatException ignored) {
                    // try next
                }
            }
        }
        return 0.0d;
    }

    private Instant parseInstant(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(text.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return "global";
        }
        return region.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
    }

    private String truncate(String body) {
        String normalized = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 220) {
            return normalized;
        }
        return normalized.substring(0, 220) + "...";
    }

    private String stripTrailingSlash(String raw) {
        String value = raw == null || raw.isBlank() ? DEFAULT_BASE_URL : raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    @FunctionalInterface
    private interface JsonSupplier {

        JsonNode get();
    }

    private record CacheEntry(JsonNode payload, Instant expiresAt, Instant fetchedAt) {

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
