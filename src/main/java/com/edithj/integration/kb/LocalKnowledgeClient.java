package com.edithj.integration.kb;

import java.util.List;

/**
 * Contract for EDITH local KB search/index operations (RAG-ready scaffolding).
 */
public interface LocalKnowledgeClient {

    List<KnowledgeChunk> semanticSearch(String query, int topK);

    void refreshIndexAsync();

    String statusSummary();
}
