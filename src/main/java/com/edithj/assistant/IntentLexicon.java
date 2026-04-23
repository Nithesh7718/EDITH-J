package com.edithj.assistant;

import java.util.Locale;

/**
 * Shared lexical rules for EDITH intent detection.
 *
 * <p>
 * Keeps phrase/keyword matching consistent across the newer classifier and
 * legacy router paths.
 */
public final class IntentLexicon {

    private IntentLexicon() {
    }

    public static boolean hasOpenVerbPrefix(String input) {
        return startsWithAny(input, "open", "launch", "start", "run");
    }

    public static boolean hasCloseVerbPrefix(String input) {
        return startsWithAny(input, "close", "quit", "terminate", "exit", "stop");
    }

    public static String stripOpenVerbPrefix(String input) {
        return stripLeadingPhrase(input, "open", "launch", "start", "run");
    }

    public static String stripCloseVerbPrefix(String input) {
        return stripLeadingPhrase(input, "close", "quit", "terminate", "exit", "stop");
    }

    public static boolean looksLikeDesktopToolsRequest(String input) {
        return containsAny(input, "desktop tools", "system tools", "capabilities", "what can you do")
                || containsAny(input, "task", "reminder", "note", "notes", "todo", "to do");
    }

    public static boolean looksLikeWorldMarketsRequest(String input) {
        return containsAny(input, "markets", "market mood", "stocks", "indices", "tickers", "crypto");
    }

    public static boolean looksLikeWorldRiskRequest(String input) {
        return containsAny(input, "risk for", "country risk", "instability", "country instability", "geopolitical risk");
    }

    public static boolean looksLikeWorldRequest(String input) {
        return containsAny(input, "world news", "global situation", "conflict", "war", "global events", "world events", "geopolitics");
    }

    public static boolean looksLikeLocalKbRequest(String input) {
        return containsAny(input, "in my docs", "in our project", "in my notes", "local kb", "knowledge base", "our documents");
    }

    public static boolean looksLikeWebRequest(String input) {
        return containsAny(input, "search web", "on the web", "internet", "online");
    }

    public static boolean containsAny(String input, String... terms) {
        String normalized = normalizeForContains(input);
        for (String term : terms) {
            if (normalized.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithAny(String input, String... prefixes) {
        String normalized = normalizeForStartsWith(input);
        for (String prefix : prefixes) {
            if (normalized.equals(prefix) || normalized.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    private static String stripLeadingPhrase(String input, String... phrases) {
        String normalized = normalizeForStartsWith(input);
        for (String phrase : phrases) {
            if (normalized.equals(phrase)) {
                return "";
            }
            if (normalized.startsWith(phrase + " ")) {
                return normalized.substring(phrase.length()).trim();
            }
        }
        return normalized;
    }

    private static String normalizeForContains(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT).trim();
    }

    private static String normalizeForStartsWith(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
