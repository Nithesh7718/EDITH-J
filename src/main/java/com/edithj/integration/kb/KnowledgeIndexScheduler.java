package com.edithj.integration.kb;

/**
 * Scheduling hook for periodic/on-demand local index refresh jobs.
 */
public interface KnowledgeIndexScheduler {

    void triggerRefreshNow();

    void start();

    void stop();
}
