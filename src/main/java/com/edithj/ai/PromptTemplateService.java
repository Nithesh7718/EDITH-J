package com.edithj.ai;

import java.util.List;

import com.edithj.config.AppConfig;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.memory.MemoryEntry;

public class PromptTemplateService {

    private final AppConfig appConfig;
    private final PromptBuilder promptBuilder;

    public PromptTemplateService() {
        this(AppConfig.load(), new PromptBuilder());
    }

    public PromptTemplateService(AppConfig appConfig, PromptBuilder promptBuilder) {
        this.appConfig = appConfig;
        this.promptBuilder = promptBuilder;
    }

    public String systemPrompt() {
        return promptBuilder.loadSystemPrompt();
    }

    public String configuredSystemPromptPath() {
        return appConfig.systemPromptPath();
    }

    public String systemPromptWithMemory(List<MemoryEntry> memoryEntries) {
        String basePrompt = systemPrompt();
        if (memoryEntries == null || memoryEntries.isEmpty()) {
            return basePrompt;
        }

        StringBuilder enriched = new StringBuilder(basePrompt);
        enriched.append("\n\nRelevant local memory:\n");
        int limit = Math.min(8, memoryEntries.size());
        for (int i = 0; i < limit; i++) {
            MemoryEntry entry = memoryEntries.get(i);
            enriched.append("- [")
                    .append(entry.category())
                    .append("] ")
                    .append(entry.content())
                    .append("\n");
        }
        return enriched.toString();
    }
}
