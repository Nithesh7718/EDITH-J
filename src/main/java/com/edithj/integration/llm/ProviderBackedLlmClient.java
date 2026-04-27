package com.edithj.integration.llm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.ai.ChatProvider;
import com.edithj.ai.ProviderFactory;

public final class ProviderBackedLlmClient implements LlmClient {

    private static final Pattern PROVIDER_OVERRIDE_PATTERN = Pattern.compile(
            "(?is)^\\s*\\[\\[provider:(groq|gemini|openai|sarvam)\\]\\]\\s*(.*)$");
    private static final Pattern NATURAL_PROVIDER_OVERRIDE_PATTERN = Pattern.compile(
            "(?is)^\\s*(?:for\\s+this\\s+answer\\s+)?use\\s+(groq|gemini|openai|sarvam)\\s+(?:for\\s+this\\s+answer\\s*[:,.-]?\\s*)?(.*)$");

    private final ProviderFactory providerFactory;

    public ProviderBackedLlmClient() {
        this(new ProviderFactory());
    }

    public ProviderBackedLlmClient(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Override
    public String generateReply(String prompt) {
        String safePrompt = prompt == null ? "" : prompt;
        String overrideProvider = null;
        String effectivePrompt = safePrompt;

        Matcher matcher = PROVIDER_OVERRIDE_PATTERN.matcher(safePrompt);
        if (matcher.matches()) {
            overrideProvider = matcher.group(1);
            effectivePrompt = matcher.group(2);
        } else {
            Matcher naturalMatcher = NATURAL_PROVIDER_OVERRIDE_PATTERN.matcher(safePrompt);
            if (naturalMatcher.matches()) {
                overrideProvider = naturalMatcher.group(1);
                effectivePrompt = naturalMatcher.group(2);
            }
        }

        ChatProvider provider = overrideProvider == null
                ? providerFactory.createProvider()
                : providerFactory.createProvider(overrideProvider);

        return provider.generateReply(effectivePrompt);
    }
}
