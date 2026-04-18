package com.edithj.ai;

import com.edithj.config.AppConfig;
import com.edithj.integration.llm.PromptBuilder;

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
}
