package com.edithj.integration.kb;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary local KB client used until vector index + embeddings are
 * configured.
 */
public class PlaceholderLocalKnowledgeClient implements LocalKnowledgeClient {

    private static final Logger logger = LoggerFactory.getLogger(PlaceholderLocalKnowledgeClient.class);

    private final AtomicInteger indexedDocs = new AtomicInteger(0);
    private final AtomicReference<Instant> lastRefresh = new AtomicReference<>(null);

    @Override
    public List<KnowledgeChunk> semanticSearch(String query, int topK) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        return List.of(new KnowledgeChunk(
                "kb-placeholder-1",
                "local-kb-placeholder",
                "Local KB is not configured yet. Add document ingestion + embeddings + vector store to enable semantic retrieval.",
                0.10d));
    }

    @Override
    public void refreshIndexAsync() {
        CompletableFuture.runAsync(() -> {
            logger.info("Local KB refresh requested (placeholder mode)");
            lastRefresh.set(Instant.now());
        });
    }

    @Override
    public String statusSummary() {
        Instant refreshed = lastRefresh.get();
        if (refreshed == null) {
            return "Local KB: placeholder mode, not indexed yet.";
        }
        return "Local KB: " + indexedDocs.get() + " docs indexed, last refresh " + refreshed + ".";
    }
}
