package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.edithj.commands.CommandHandler;
import com.edithj.integration.kb.KnowledgeChunk;
import com.edithj.integration.kb.LocalKnowledgeClient;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.integration.worldmonitor.CountryInstability;
import com.edithj.integration.worldmonitor.MarketSnapshot;
import com.edithj.integration.worldmonitor.WorldMonitorClient;
import com.edithj.launcher.AppNameResolver;

class KnowledgeRouterTest {

    @Test
    void route_openAppWithWhatsappAliasDelegatesToWhatsappHandler() {
        IntentRouter legacyRouter = new IntentRouter();
        legacyRouter.registerHandler(new StaticHandler(IntentType.WHATSAPP, "whatsapp handler called"));

        KnowledgeRouter router = new KnowledgeRouter(
                legacyRouter,
                new FallbackChatService(prompt -> "fallback", new PromptBuilder(), 8),
                prompt -> "unused",
                new ConfigurableWorldMonitorClient(false),
                new StaticLocalKnowledgeClient(List.of()),
                new AppNameResolver());

        IntentClassifier.Classification classification = new IntentClassifier.Classification(
                Intent.OPEN_APP,
                List.of("what's up"),
                "open what's up",
                false,
                List.of(Intent.OPEN_APP),
                0.90d);

        AssistantResponse response = router.route(classification, "typed");

        assertEquals(IntentType.WHATSAPP, response.intentType());
        assertEquals("whatsapp handler called", response.answer());
    }

    @Test
    void route_worldIntentWithoutConfigurationReturnsSetupGuidance() {
        IntentRouter legacyRouter = new IntentRouter();
        KnowledgeRouter router = new KnowledgeRouter(
                legacyRouter,
                new FallbackChatService(prompt -> "fallback", new PromptBuilder(), 8),
                prompt -> "unused",
                new ConfigurableWorldMonitorClient(false),
                new StaticLocalKnowledgeClient(List.of()),
                new AppNameResolver());

        IntentClassifier.Classification classification = new IntentClassifier.Classification(
                Intent.ASK_WORLD,
                List.of(),
                "world news",
                false,
                List.of(Intent.ASK_WORLD),
                0.78d);

        AssistantResponse response = router.route(classification, "typed");

        assertEquals(IntentType.ASK_WORLD, response.intentType());
        assertTrue(response.answer().contains("WORLD_MONITOR_API_KEY"));
    }

    @Test
    void route_localKbIntentSynthesizesAnswerFromRetrievedChunks() {
        IntentRouter legacyRouter = new IntentRouter();
        LlmClient llmClient = prompt -> "Using local docs: this is the synthesized answer.";
        KnowledgeRouter router = new KnowledgeRouter(
                legacyRouter,
                new FallbackChatService(prompt -> "fallback", new PromptBuilder(), 8),
                llmClient,
                new ConfigurableWorldMonitorClient(false),
                new StaticLocalKnowledgeClient(List.of(
                        new KnowledgeChunk("1", "docs/architecture.md", "EDITH uses a unified transcript.", 0.91d))),
                new AppNameResolver());

        IntentClassifier.Classification classification = new IntentClassifier.Classification(
                Intent.ASK_LOCAL_KB,
                List.of(),
                "in our documents how does transcript work",
                false,
                List.of(Intent.ASK_LOCAL_KB),
                0.78d);

        AssistantResponse response = router.route(classification, "typed");

        assertEquals(IntentType.ASK_LOCAL_KB, response.intentType());
        assertTrue(response.answer().contains("synthesized answer"));
    }

    @Test
    void route_localKbIntentWithNoChunksReturnsGuidanceMessage() {
        AssistantTelemetry.instance().reset();
        IntentRouter legacyRouter = new IntentRouter();
        KnowledgeRouter router = new KnowledgeRouter(
                legacyRouter,
                new FallbackChatService(prompt -> "fallback", new PromptBuilder(), 8),
                prompt -> "unused",
                new ConfigurableWorldMonitorClient(false),
                new StaticLocalKnowledgeClient(List.of()),
                new AppNameResolver());

        IntentClassifier.Classification classification = new IntentClassifier.Classification(
                Intent.ASK_LOCAL_KB,
                List.of(),
                "in our project how does transcript work",
                false,
                List.of(Intent.ASK_LOCAL_KB),
                0.78d);

        AssistantResponse response = router.route(classification, "typed");

        assertEquals(IntentType.ASK_LOCAL_KB, response.intentType());
        assertTrue(response.answer().contains("couldn't find relevant local knowledge"));
        assertEquals(1L, AssistantTelemetry.instance().snapshot().localKbEmptyHits());
    }

    @Test
    void route_worldIntentOpensCircuitAfterRepeatedFailures() {
        AssistantTelemetry.instance().reset();
        IntentRouter legacyRouter = new IntentRouter();
        KnowledgeRouter router = new KnowledgeRouter(
                legacyRouter,
                new FallbackChatService(prompt -> "fallback", new PromptBuilder(), 8),
                prompt -> "unused",
                new FailingWorldMonitorClient(),
                new StaticLocalKnowledgeClient(List.of()),
                new AppNameResolver());

        IntentClassifier.Classification classification = new IntentClassifier.Classification(
                Intent.ASK_WORLD,
                List.of(),
                "world news",
                false,
                List.of(Intent.ASK_WORLD),
                0.78d);

        AssistantResponse first = router.route(classification, "typed");
        AssistantResponse second = router.route(classification, "typed");
        AssistantResponse third = router.route(classification, "typed");
        AssistantResponse fourth = router.route(classification, "typed");

        assertTrue(first.answer().contains("could not fetch World Monitor data"));
        assertTrue(second.answer().contains("could not fetch World Monitor data"));
        assertTrue(third.answer().contains("could not fetch World Monitor data"));
        assertTrue(fourth.answer().contains("cooling down"));
        assertEquals(1L, AssistantTelemetry.instance().snapshot().worldCircuitOpenHits());
    }

    private static final class StaticHandler implements CommandHandler {

        private final IntentType intentType;
        private final String response;

        private StaticHandler(IntentType intentType, String response) {
            this.intentType = intentType;
            this.response = response;
        }

        @Override
        public IntentType intentType() {
            return intentType;
        }

        @Override
        public String handle(CommandContext context) {
            return response;
        }
    }

    private static final class StaticLocalKnowledgeClient implements LocalKnowledgeClient {

        private final List<KnowledgeChunk> chunks;

        private StaticLocalKnowledgeClient(List<KnowledgeChunk> chunks) {
            this.chunks = chunks;
        }

        @Override
        public List<KnowledgeChunk> semanticSearch(String query, int topK) {
            return chunks;
        }

        @Override
        public void refreshIndexAsync() {
            // no-op for tests
        }

        @Override
        public String statusSummary() {
            return "test";
        }
    }

    private static final class ConfigurableWorldMonitorClient extends WorldMonitorClient {

        private final boolean configured;

        private ConfigurableWorldMonitorClient(boolean configured) {
            this.configured = configured;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public CountryInstability getCountryInstability(String isoCode) {
            return new CountryInstability("GLOBAL", 0.0d, "low", "test");
        }

        @Override
        public MarketSnapshot getMarketSnapshot(List<String> symbols) {
            return new MarketSnapshot(java.time.Instant.now(), List.of());
        }
    }

    private static final class FailingWorldMonitorClient extends WorldMonitorClient {

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public java.util.List<com.edithj.integration.worldmonitor.ConflictEvent> getRecentConflicts(String region, int limit) {
            throw new IllegalStateException("simulated world monitor outage");
        }
    }
}
