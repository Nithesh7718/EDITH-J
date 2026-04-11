package com.edithj.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.edithj.integration.llm.PromptBuilder;

class PromptBuilderTest {

    @Test
    void loadSystemPrompt_readsPromptResource() {
        PromptBuilder promptBuilder = new PromptBuilder();

        String prompt = promptBuilder.loadSystemPrompt();

        assertFalse(prompt.isBlank());
    }
}
