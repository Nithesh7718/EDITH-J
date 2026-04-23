package com.edithj.launcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves user phrasing to canonical app targets before launch attempts.
 */
public final class AppNameResolver {

    private final Map<String, List<String>> canonicalToSynonyms;

    public AppNameResolver() {
        this(defaultMappings());
    }

    public AppNameResolver(Map<String, List<String>> mappings) {
        this.canonicalToSynonyms = new LinkedHashMap<>();
        if (mappings != null) {
            mappings.forEach((canonicalId, synonyms) -> this.canonicalToSynonyms.put(
                    normalize(canonicalId),
                    sanitizeSynonyms(synonyms)));
        }
    }

    public String resolveLaunchTarget(String rawInput) {
        String normalized = normalize(rawInput);
        if (normalized.isBlank()) {
            return "";
        }

        String bestCanonical = "";
        int bestScore = 0;

        for (Map.Entry<String, List<String>> entry : canonicalToSynonyms.entrySet()) {
            String canonical = entry.getKey();
            for (String synonym : entry.getValue()) {
                if (synonym.isBlank()) {
                    continue;
                }

                int score = scoreMatch(normalized, synonym);
                if (score > bestScore) {
                    bestScore = score;
                    bestCanonical = canonical;
                }
            }
        }

        if (bestScore == 0) {
            return normalized;
        }

        return switch (bestCanonical) {
            case "whatsapp" ->
                "whatsapp";
            case "youtube" ->
                "youtube";
            case "file explorer" ->
                "explorer";
            default ->
                normalized;
        };
    }

    private int scoreMatch(String input, String synonym) {
        if (input.equals(synonym)) {
            return 100;
        }
        if (input.contains(synonym)) {
            return Math.min(95, 50 + synonym.length());
        }
        if (synonym.contains(input)) {
            return Math.min(80, 40 + input.length());
        }

        int distance = levenshtein(input, synonym);
        int maxLen = Math.max(input.length(), synonym.length());
        if (maxLen == 0) {
            return 0;
        }

        double similarity = 1.0d - ((double) distance / (double) maxLen);
        if (similarity < 0.72d) {
            return 0;
        }

        return (int) Math.round(similarity * 70.0d);
    }

    private int levenshtein(String left, String right) {
        int[] costs = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            costs[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            costs[0] = i;
            int previousDiagonal = i - 1;
            for (int j = 1; j <= right.length(); j++) {
                int temp = costs[j];
                int substitution = previousDiagonal + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                int insertion = costs[j] + 1;
                int deletion = costs[j - 1] + 1;
                costs[j] = Math.min(Math.min(insertion, deletion), substitution);
                previousDiagonal = temp;
            }
        }

        return costs[right.length()];
    }

    private List<String> sanitizeSynonyms(List<String> synonyms) {
        List<String> cleaned = new ArrayList<>();
        if (synonyms != null) {
            for (String synonym : synonyms) {
                String normalized = normalize(synonym);
                if (!normalized.isBlank()) {
                    cleaned.add(normalized);
                }
            }
        }
        return cleaned;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s']", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Map<String, List<String>> defaultMappings() {
        Map<String, List<String>> mapping = new LinkedHashMap<>();
        mapping.put("whatsapp", List.of("whatsapp", "whats app", "whatsup", "whats up", "what's up", "whtsapp", "whatsap", "watsapp"));
        mapping.put("youtube", List.of("youtube", "yt", "you tube"));
        mapping.put("file_explorer", List.of("file explorer", "explorer", "my files", "file manager"));
        return mapping;
    }
}
