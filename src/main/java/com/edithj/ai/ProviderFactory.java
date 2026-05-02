package com.edithj.ai;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.edithj.ai.AiConfig.Provider;

public final class ProviderFactory {

    private final AiConfig config;
    private final HttpClient httpClient;

    public ProviderFactory() {
        this(AiConfig.load(), HttpClient.newHttpClient());
    }

    public ProviderFactory(AiConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    ProviderFactory(AiConfig config, HttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public ChatProvider createProvider() {
        return createProvider(config.selectedProvider().id());
    }

    public ChatProvider createProvider(String providerOverride) {
        Provider provider = Provider.from(providerOverride == null || providerOverride.isBlank()
                ? config.selectedProvider().id()
                : providerOverride.toLowerCase(Locale.ROOT));

        String apiKey = config.resolveApiKey(provider);
        if (apiKey == null || apiKey.isBlank()) {
            return new MissingApiKeyChatProvider(provider.id(),
                    ""
                    + capitalize(provider.id())
                    + " is not configured. Set "
                    + envName(provider)
                    + " in your environment (preferred), or use "
                    + propertyName(provider)
                    + " in edith.properties.");
        }

        return switch (provider) {
            case GROQ ->
                new GroqChatProvider(config, httpClient);
            case GEMINI ->
                new GeminiChatProvider(config, httpClient);
            case OPENAI ->
                new OpenAIChatProvider(config, httpClient);
            case SARVAM ->
                new SarvamChatProvider(config, httpClient);
        };
    }

    private String envName(Provider provider) {
        return switch (provider) {
            case GROQ ->
                "GROQ_API_KEY";
            case GEMINI ->
                "GEMINI_API_KEY";
            case OPENAI ->
                "OPENAI_API_KEY";
            case SARVAM ->
                "SARVAM_API_KEY";
        };
    }

    private String propertyName(Provider provider) {
        return switch (provider) {
            case GROQ ->
                "edith.ai.groq.apiKey";
            case GEMINI ->
                "edith.ai.gemini.apiKey";
            case OPENAI ->
                "edith.ai.openai.apiKey";
            case SARVAM ->
                "edith.ai.sarvam.apiKey";
        };
    }

    public String defaultProviderId() {
        return config.selectedProvider().id();
    }

    public List<String> supportedProviderIds() {
        return List.of(Provider.GROQ.id(), Provider.GEMINI.id(), Provider.OPENAI.id(), Provider.SARVAM.id());
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Provider";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
