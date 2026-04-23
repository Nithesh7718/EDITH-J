package com.edithj.integration.kb;

import java.util.Objects;

/**
 * Placeholder scheduler implementation; real periodic jobs can be added later.
 */
public class NoopKnowledgeIndexScheduler implements KnowledgeIndexScheduler {

    private final LocalKnowledgeClient localKnowledgeClient;

    public NoopKnowledgeIndexScheduler(LocalKnowledgeClient localKnowledgeClient) {
        this.localKnowledgeClient = Objects.requireNonNull(localKnowledgeClient, "localKnowledgeClient");
    }

    @Override
    public void triggerRefreshNow() {
        localKnowledgeClient.refreshIndexAsync();
    }

    @Override
    public void start() {
        // Scheduling intentionally deferred.
    }

    @Override
    public void stop() {
        // Scheduling intentionally deferred.
    }
}
