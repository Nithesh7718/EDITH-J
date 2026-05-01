package com.edithj.assistant;

import java.util.List;
import java.util.Objects;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.edithj.integration.kb.KnowledgeChunk;
import com.edithj.integration.kb.LocalKnowledgeClient;
import com.edithj.integration.kb.PlaceholderLocalKnowledgeClient;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.worldmonitor.CountryInstability;
import com.edithj.integration.worldmonitor.MarketSnapshot;
import com.edithj.integration.worldmonitor.WorldMonitorClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Routes classified EDITH intents across command handlers and knowledge hubs.
 */
public class KnowledgeRouter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int WORLD_MONITOR_FAILURE_THRESHOLD = 3;
    private static final Duration WORLD_MONITOR_COOLDOWN = Duration.ofSeconds(45);

    private final IntentRouter legacyIntentRouter;
    private final FallbackChatService fallbackChatService;
    private final LlmClient llmClient;
    private final WorldMonitorClient worldMonitorClient;
    private final LocalKnowledgeClient localKnowledgeClient;
    private final com.edithj.launcher.AppNameResolver appNameResolver;
    private final AssistantTelemetry telemetry = AssistantTelemetry.instance();
    private final AtomicInteger worldMonitorConsecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<Instant> worldMonitorCircuitUntil = new AtomicReference<>(null);

    public KnowledgeRouter(IntentRouter legacyIntentRouter,
            FallbackChatService fallbackChatService,
            LlmClient llmClient,
            WorldMonitorClient worldMonitorClient,
            LocalKnowledgeClient localKnowledgeClient,
            com.edithj.launcher.AppNameResolver appNameResolver) {
        this.legacyIntentRouter = Objects.requireNonNull(legacyIntentRouter, "legacyIntentRouter");
        this.fallbackChatService = Objects.requireNonNull(fallbackChatService, "fallbackChatService");
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
        this.worldMonitorClient = Objects.requireNonNull(worldMonitorClient, "worldMonitorClient");
        this.localKnowledgeClient = Objects.requireNonNull(localKnowledgeClient, "localKnowledgeClient");
        this.appNameResolver = Objects.requireNonNull(appNameResolver, "appNameResolver");
    }

    public KnowledgeRouter(IntentRouter legacyIntentRouter, FallbackChatService fallbackChatService, LlmClient llmClient) {
        this(legacyIntentRouter,
                fallbackChatService,
                llmClient,
                new WorldMonitorClient(),
                new PlaceholderLocalKnowledgeClient(),
                new com.edithj.launcher.AppNameResolver());
    }

    public AssistantResponse route(IntentClassifier.Classification classification, String channel) {
        String safeChannel = (channel == null || channel.isBlank()) ? "typed" : channel.trim();
        if (classification == null || classification.normalizedInput().isBlank()) {
            return new AssistantResponse(IntentType.FALLBACK_CHAT, "", "Please enter a message.", safeChannel);
        }

        Intent intent = classification.intent();
        String input = classification.normalizedInput();

        if (intent == Intent.OPEN_APP) {
            return handleOpenApp(input, classification.targets(), safeChannel);
        }

        if (intent == Intent.CLOSE_APP) {
            return new AssistantResponse(
                    IntentType.DESKTOP_TOOLS,
                    input,
                    "Close-app automation is scaffolded but not yet wired for safe OS process termination.",
                    safeChannel);
        }

        if (intent == Intent.ASK_WORLD || intent == Intent.ASK_WORLD_RISK || intent == Intent.ASK_WORLD_MARKETS) {
            return handleWorldIntent(intent, input, classification.targets(), safeChannel);
        }

        if (intent == Intent.ASK_LOCAL_KB) {
            return handleLocalKbIntent(input, safeChannel);
        }

        if (intent == Intent.ASK_WEB) {
            return handleAskWeb(input, safeChannel);
        }

        IntentRouter.RoutedIntent legacyRouted = legacyIntentRouter.route(input);
        if (legacyRouted.intentType() != IntentType.FALLBACK_CHAT) {
            return legacyIntentRouter.routeAndHandle(legacyRouted, safeChannel);
        }

        String fallback = fallbackChatService.runFallbackChat(safeChannel);
        return new AssistantResponse(IntentType.GENERAL_CHAT, input, fallback, safeChannel);
    }

    private AssistantResponse handleOpenApp(String input, List<String> targets, String channel) {
        String candidate = targets.isEmpty() ? input : targets.get(0);
        String launchTarget = appNameResolver.resolveLaunchTarget(candidate);

        if (isWhatsAppAlias(launchTarget)) {
            IntentRouter.RoutedIntent whatsappRouted = new IntentRouter.RoutedIntent(
                    IntentType.WHATSAPP,
                    input,
                    input);
            return legacyIntentRouter.routeAndHandle(whatsappRouted, channel);
        }

        IntentRouter.RoutedIntent routed = new IntentRouter.RoutedIntent(
                IntentType.APP_LAUNCH,
                input,
                launchTarget);

        return legacyIntentRouter.routeAndHandle(routed, channel);
    }

    private boolean isWhatsAppAlias(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.contains("whatsapp")
                || normalized.contains("whtsapp")
                || normalized.contains("whatsap")
                || normalized.contains("watsapp");
    }

    private AssistantResponse handleWorldIntent(Intent intent, String input, List<String> targets, String channel) {
        if (!worldMonitorClient.isConfigured()) {
            return new AssistantResponse(
                    mapIntentType(intent),
                    input,
                    "World Monitor is not configured. Set WORLD_MONITOR_API_KEY (and optional WORLD_MONITOR_BASE_URL) to enable live world intelligence.",
                    channel);
        }
        if (isWorldMonitorCircuitOpen()) {
            telemetry.recordWorldCircuitOpenHit();
            return new AssistantResponse(
                    mapIntentType(intent),
                    input,
                    "World Monitor is temporarily cooling down after repeated failures. Please retry in under a minute.",
                    channel);
        }

        try {
            String answer;
            switch (intent) {
                case ASK_WORLD_RISK -> {
                    String isoCode = targets.isEmpty() ? "GLOBAL" : targets.get(0);
                    CountryInstability instability = worldMonitorClient.getCountryInstability(isoCode);
                    answer = synthesizeFromHubSafely(
                            "You are EDITH, a desktop AI assistant. You have trusted world data from World Monitor (real-time global intelligence). Use it to answer this question accurately and clearly.",
                            input,
                            toPrettyJson(instability),
                            "I received world-risk data but couldn't summarize it right now.");
                }
                case ASK_WORLD_MARKETS -> {
                    MarketSnapshot snapshot = worldMonitorClient.getMarketSnapshot(targets.isEmpty() ? List.of("SPY", "QQQ", "BTC-USD") : targets);
                    answer = synthesizeFromHubSafely(
                            "You are EDITH, a desktop AI assistant. You have trusted world data from World Monitor (real-time global intelligence). Use it to answer this question accurately and clearly.",
                            input,
                            toPrettyJson(snapshot),
                            "I received market data but couldn't summarize it right now.");
                }
                case ASK_WORLD -> {
                    String region = targets.isEmpty() ? "global" : targets.get(0);
                    var conflicts = worldMonitorClient.getRecentConflicts(region, 10);
                    answer = synthesizeFromHubSafely(
                            "You are EDITH, a desktop AI assistant. You have trusted world data from World Monitor (real-time global intelligence). Use it to answer this question accurately and clearly.",
                            input,
                            toPrettyJson(conflicts),
                            "I received world updates but couldn't summarize them right now.");
                }
                default -> {
                    String region = targets.isEmpty() ? "global" : targets.get(0);
                    var conflicts = worldMonitorClient.getRecentConflicts(region, 10);
                    answer = synthesizeFromHubSafely(
                            "You are EDITH, a desktop AI assistant. You have trusted world data from World Monitor (real-time global intelligence). Use it to answer this question accurately and clearly.",
                            input,
                            toPrettyJson(conflicts),
                            "I received world updates but couldn't summarize them right now.");
                }
            }

            worldMonitorConsecutiveFailures.set(0);
            return new AssistantResponse(mapIntentType(intent), input, answer + "\n\nSource: World Monitor", channel);
        } catch (RuntimeException exception) {
            openWorldMonitorCircuitIfNeeded();
            return new AssistantResponse(
                    mapIntentType(intent),
                    input,
                    "I could not fetch World Monitor data right now: " + exception.getMessage()
                    + " Please retry shortly, or ask for a general EDITH summary without live data.",
                    channel);
        }
    }

    private AssistantResponse handleLocalKbIntent(String input, String channel) {
        List<KnowledgeChunk> chunks = localKnowledgeClient.semanticSearch(input, 6);
        if (chunks == null || chunks.isEmpty()) {
            telemetry.recordLocalKbEmptyHit();
            return new AssistantResponse(
                    IntentType.ASK_LOCAL_KB,
                    input,
                    "I couldn't find relevant local knowledge yet. Try a narrower query, or refresh the local index first.",
                    channel);
        }

        String contextJson = toPrettyJson(chunks);
        String answer = synthesizeFromHubSafely(
                "You are EDITH. Use the following EDITH local knowledge base snippets to answer the user's question.",
                input,
                contextJson,
                "I found local knowledge snippets, but synthesis failed. Please try a shorter query.");

        if (answer.isBlank()) {
            answer = "Local KB is not configured yet. I can scaffold retrieval, but document indexing/embeddings are still pending.";
        }

        return new AssistantResponse(IntentType.ASK_LOCAL_KB, input, answer, channel);
    }

    private AssistantResponse handleAskWeb(String input, String channel) {
        String prompt = """
                You are EDITH, a desktop AI assistant.
                User request: %s

                Provide a concise answer. If the request needs live web browsing that is unavailable,
                say so briefly and suggest a focused query.
                """.formatted(input);
        String answer = safeGenerateReply(prompt, "");
        if (answer.isBlank()) {
            answer = "I couldn't complete a live web answer right now. Try a focused query like: "
                    + "\"search web java 21 virtual threads performance\".";
        }
        return new AssistantResponse(IntentType.ASK_WEB, input, answer.trim(), channel);
    }

    private String synthesizeFromHubSafely(String systemPrompt, String userQuestion, String hubJson, String fallbackAnswer) {
        String prompt = """
                %s

                User question:
                %s

                Trusted structured data (JSON):
                %s

                Return a concise, structured answer with key points first.
                """.formatted(systemPrompt, userQuestion, hubJson);
        String reply = safeGenerateReply(prompt, fallbackAnswer);
        return reply == null ? "" : reply.trim();
    }

    private String safeGenerateReply(String prompt, String fallbackAnswer) {
        try {
            String reply = llmClient.generateReply(prompt);
            if (reply == null || reply.isBlank()) {
                return fallbackAnswer == null ? "" : fallbackAnswer;
            }
            return reply.trim();
        } catch (RuntimeException exception) {
            return fallbackAnswer == null ? "" : fallbackAnswer;
        }
    }

    private boolean isWorldMonitorCircuitOpen() {
        Instant until = worldMonitorCircuitUntil.get();
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            worldMonitorCircuitUntil.compareAndSet(until, null);
            worldMonitorConsecutiveFailures.set(0);
            return false;
        }
        return true;
    }

    private void openWorldMonitorCircuitIfNeeded() {
        int failures = worldMonitorConsecutiveFailures.incrementAndGet();
        if (failures >= WORLD_MONITOR_FAILURE_THRESHOLD) {
            worldMonitorCircuitUntil.set(Instant.now().plus(WORLD_MONITOR_COOLDOWN));
        }
    }

    private IntentType mapIntentType(Intent intent) {
        return switch (intent) {
            case OPEN_APP ->
                IntentType.APP_LAUNCH;
            case CLOSE_APP, DESKTOP_TOOLS ->
                IntentType.DESKTOP_TOOLS;
            case ASK_WORLD ->
                IntentType.ASK_WORLD;
            case ASK_WORLD_RISK ->
                IntentType.ASK_WORLD_RISK;
            case ASK_WORLD_MARKETS ->
                IntentType.ASK_WORLD_MARKETS;
            case ASK_LOCAL_KB ->
                IntentType.ASK_LOCAL_KB;
            case ASK_WEB ->
                IntentType.ASK_WEB;
            case GENERAL_CHAT ->
                IntentType.GENERAL_CHAT;
        };
    }

    private String toPrettyJson(Object value) {
        if (value == null) {
            return "{}";
        }

        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
