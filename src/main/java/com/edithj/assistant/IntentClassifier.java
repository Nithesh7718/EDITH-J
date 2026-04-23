package com.edithj.assistant;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.integration.llm.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Two-stage classifier for EDITH intents: rule-based first, then LLM refinement
 * if ambiguous.
 */
public class IntentClassifier {

    private static final Logger logger = LoggerFactory.getLogger(IntentClassifier.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern OPEN_PREFIX = Pattern.compile("^(open|launch|start|run)\\b");
    private static final Pattern CLOSE_PREFIX = Pattern.compile("^(close|quit|terminate|exit|stop)\\b");

    private final LlmClient llmClient;

    public IntentClassifier(LlmClient llmClient) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
    }

    public Classification classify(String rawInput) {
        String normalized = normalize(rawInput);
        if (normalized.isBlank()) {
            return new Classification(Intent.GENERAL_CHAT, List.of(), "", false, List.of(Intent.GENERAL_CHAT));
        }

        RuleEvaluation ruleEvaluation = stageOneRules(normalized);
        Intent finalIntent = ruleEvaluation.primary();
        List<String> targets = new ArrayList<>(ruleEvaluation.targets());
        boolean refinedByLlm = false;

        if (ruleEvaluation.ambiguous()) {
            LlmEvaluation llmEvaluation = stageTwoLlmRefinement(normalized);
            if (llmEvaluation != null && llmEvaluation.intent() != null) {
                finalIntent = llmEvaluation.intent();
                if (!llmEvaluation.targets().isEmpty()) {
                    targets = llmEvaluation.targets();
                }
                refinedByLlm = true;
            }
        }

        return new Classification(finalIntent, List.copyOf(targets), normalized, refinedByLlm, ruleEvaluation.candidates());
    }

    private RuleEvaluation stageOneRules(String normalized) {
        String lower = normalized.toLowerCase(Locale.ROOT);
        Set<Intent> candidates = new LinkedHashSet<>();
        List<String> targets = new ArrayList<>();

        if (OPEN_PREFIX.matcher(lower).find()) {
            candidates.add(Intent.OPEN_APP);
            String appTarget = lower.replaceFirst("^(open|launch|start|run)\\s+", "").trim();
            if (!appTarget.isBlank()) {
                targets.add(appTarget);
            }
        }

        if (CLOSE_PREFIX.matcher(lower).find()) {
            candidates.add(Intent.CLOSE_APP);
            String appTarget = lower.replaceFirst("^(close|quit|terminate|exit|stop)\\s+", "").trim();
            if (!appTarget.isBlank()) {
                targets.add(appTarget);
            }
        }

        if (containsAny(lower, "desktop tools", "system tools", "capabilities", "what can you do")) {
            candidates.add(Intent.DESKTOP_TOOLS);
        }

        if (containsAny(lower, "task", "reminder", "note", "notes", "todo", "to do")) {
            candidates.add(Intent.DESKTOP_TOOLS);
        }

        if (containsAny(lower, "markets", "market mood", "stocks", "indices", "tickers", "crypto")) {
            candidates.add(Intent.ASK_WORLD_MARKETS);
        }

        if (containsAny(lower, "risk for", "country risk", "instability", "country instability", "geopolitical risk")) {
            candidates.add(Intent.ASK_WORLD_RISK);
        }

        if (containsAny(lower, "world news", "global situation", "conflict", "war", "global events", "world events", "geopolitics")) {
            candidates.add(Intent.ASK_WORLD);
        }

        if (containsAny(lower, "in my docs", "in our project", "in my notes", "local kb", "knowledge base", "our documents")) {
            candidates.add(Intent.ASK_LOCAL_KB);
        }

        if (containsAny(lower, "search web", "on the web", "internet", "online")) {
            candidates.add(Intent.ASK_WEB);
        }

        if (candidates.isEmpty()) {
            candidates.add(Intent.GENERAL_CHAT);
        }

        Intent primary = candidates.iterator().next();
        boolean ambiguous = candidates.size() > 1;

        return new RuleEvaluation(primary, List.copyOf(targets), List.copyOf(candidates), ambiguous);
    }

    private LlmEvaluation stageTwoLlmRefinement(String normalized) {
        String prompt = """
                You are an intent classifier for a desktop assistant named EDITH.
                Given the user input, return JSON with fields:
                - intent (one of: OPEN_APP, CLOSE_APP, DESKTOP_TOOLS, ASK_WORLD, ASK_WORLD_RISK, ASK_WORLD_MARKETS, ASK_LOCAL_KB, ASK_WEB, GENERAL_CHAT)
                - targets (list of strings like app names, regions, tickers, doc hints)
                Do not answer the question.

                User input: %s
                """.formatted(normalized);

        try {
            String raw = llmClient.generateReply(prompt);
            if (raw == null || raw.isBlank()) {
                return null;
            }

            JsonNode root = OBJECT_MAPPER.readTree(extractJson(raw));
            String rawIntent = root.path("intent").asText("").trim();
            Intent intent = parseIntent(rawIntent);
            if (intent == null) {
                return null;
            }

            List<String> targets = new ArrayList<>();
            JsonNode targetsNode = root.path("targets");
            if (targetsNode.isArray()) {
                for (JsonNode targetNode : targetsNode) {
                    String value = targetNode.asText("").trim();
                    if (!value.isBlank()) {
                        targets.add(value);
                    }
                }
            }

            return new LlmEvaluation(intent, List.copyOf(targets));
        } catch (JsonProcessingException | RuntimeException exception) {
            logger.debug("LLM intent refinement failed; using rule-based result", exception);
            return null;
        }
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private Intent parseIntent(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Intent.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean containsAny(String input, String... terms) {
        for (String term : terms) {
            if (input.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }

        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s']", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record Classification(Intent intent, List<String> targets, String normalizedInput, boolean llmRefined, List<Intent> candidates) {

    }

    private record RuleEvaluation(Intent primary, List<String> targets, List<Intent> candidates, boolean ambiguous) {

    }

    private record LlmEvaluation(Intent intent, List<String> targets) {

    }
}
