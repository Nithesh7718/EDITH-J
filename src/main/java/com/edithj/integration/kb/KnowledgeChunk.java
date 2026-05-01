package com.edithj.integration.kb;

/**
 * Represents one semantic retrieval result from the local EDITH knowledge base.
 */
public record KnowledgeChunk(String id, String source, String content, double score) {

}
