package com.edithj.assistant;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

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

    private static final double HIGH_CONFIDENCE = 0.90d;
    private static final double MEDIUM_CONFIDENCE = 0.78d;
    private static final double LOW_CONFIDENCE = 0.45d;
    private static final double LLM_REFINED_CONFIDENCE = 0.84d;

    private final LlmClient llmClient;

    public IntentClassifier(LlmClient llmClient) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
    }

    public Classification classify(String rawInput) {
        String normalized = normalize(rawInput);
        if (normalized.isBlank()) {
            return new Classification(Intent.GENERAL_CHAT, List.of(), "", false, List.of(Intent.GENERAL_CHAT), LOW_CONFIDENCE);
        }

        RuleEvaluation ruleEvaluation = stageOneRules(normalized);
        Intent finalIntent = ruleEvaluation.primary();
        List<String> targets = new ArrayList<>(ruleEvaluation.targets());
        boolean refinedByLlm = false;
        double confidenceScore = ruleEvaluation.confidenceScore();

        if (ruleEvaluation.ambiguous()) {
            LlmEvaluation llmEvaluation = stageTwoLlmRefinement(normalized);
            if (llmEvaluation != null && llmEvaluation.intent() != null) {
                finalIntent = llmEvaluation.intent();
                if (!llmEvaluation.targets().isEmpty()) {
                    targets = llmEvaluation.targets();
                }
                refinedByLlm = true;
                confidenceScore = LLM_REFINED_CONFIDENCE;
            }
        }

        return new Classification(finalIntent, List.copyOf(targets), normalized, refinedByLlm, ruleEvaluation.candidates(), confidenceScore);
    }

    private RuleEvaluation stageOneRules(String normalized) {
        String lower = normalized.toLowerCase(Locale.ROOT);
        Set<Intent> candidates = new LinkedHashSet<>();
        List<String> targets = new ArrayList<>();

        if (IntentLexicon.hasOpenVerbPrefix(lower)) {
            candidates.add(Intent.OPEN_APP);
            String appTarget = IntentLexicon.stripOpenVerbPrefix(lower);
            if (!appTarget.isBlank()) {
                targets.add(appTarget);
            }
        }

        if (IntentLexicon.hasCloseVerbPrefix(lower)) {
            candidates.add(Intent.CLOSE_APP);
            String appTarget = IntentLexicon.stripCloseVerbPrefix(lower);
            if (!appTarget.isBlank()) {
                targets.add(appTarget);
            }
        }

        if (IntentLexicon.looksLikeDesktopToolsRequest(lower)) {
            candidates.add(Intent.DESKTOP_TOOLS);
        }

        if (IntentLexicon.looksLikeWorldMarketsRequest(lower)) {
            candidates.add(Intent.ASK_WORLD_MARKETS);
        }

        if (IntentLexicon.looksLikeWorldRiskRequest(lower)) {
            candidates.add(Intent.ASK_WORLD_RISK);
        }

        if (IntentLexicon.looksLikeWorldRequest(lower)) {
            candidates.add(Intent.ASK_WORLD);
        }

        if (IntentLexicon.looksLikeLocalKbRequest(lower)) {
            candidates.add(Intent.ASK_LOCAL_KB);
        }

        if (IntentLexicon.looksLikeWebRequest(lower)) {
            candidates.add(Intent.ASK_WEB);
        }

        if (candidates.isEmpty()) {
            candidates.add(Intent.GENERAL_CHAT);
        }

        Intent primary = candidates.iterator().next();
        boolean ambiguous = candidates.size() > 1;
        double confidenceScore = ambiguous ? LOW_CONFIDENCE : confidenceForPrimary(primary);

        return new RuleEvaluation(primary, List.copyOf(targets), List.copyOf(candidates), ambiguous, confidenceScore);
    }

    private double confidenceForPrimary(Intent primary) {
        return switch (primary) {
            case OPEN_APP, CLOSE_APP ->
                HIGH_CONFIDENCE;
            case DESKTOP_TOOLS, ASK_WORLD, ASK_WORLD_RISK, ASK_WORLD_MARKETS, ASK_LOCAL_KB, ASK_WEB ->
                MEDIUM_CONFIDENCE;
            case GENERAL_CHAT ->
                LOW_CONFIDENCE;
        };
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
            logger.debug("LLM intent refinement failed ({}); using rule-based result",
                    exception.getClass().getSimpleName());
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

    private String normalize(String input) {
        if (input == null) {
            return "";
        }

        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s']", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record Classification(Intent intent,
            List<String> targets,
            String normalizedInput,
            boolean llmRefined,
            List<Intent> candidates,
            double confidenceScore) {

        public boolean ambiguous() {
            return candidates != null && candidates.size() > 1;
        }

    }

    private record RuleEvaluation(Intent primary,
            List<String> targets,
            List<Intent> candidates,
            boolean ambiguous,
            double confidenceScore) {

    }

    private record LlmEvaluation(Intent intent, List<String> targets) {

    }
}
