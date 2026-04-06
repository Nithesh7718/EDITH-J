package com.edithj.integration.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PromptBuilder {

    public String loadSystemPrompt() {
        try (InputStream inputStream = getClass().getResourceAsStream("/prompts/system-prompt.txt")) {
            if (inputStream == null) {
                return "";
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read system prompt", exception);
        }
    }
}
