package com.edithj.integration.llm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edithj.ai.ChatProvider;
import com.edithj.ai.ProviderFactory;
import com.edithj.resilience.FailureType;
import com.edithj.resilience.HealthMonitorRegistry;
import com.edithj.resilience.HealthSignal;
import com.edithj.resilience.IncidentSeverity;

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

        List<String> providerOrder = new ArrayList<>(providerFactory.supportedProviderIds());
        if (overrideProvider != null && !overrideProvider.isBlank()) {
            providerOrder.remove(overrideProvider);
            providerOrder.add(0, overrideProvider);
        }

        String lastReason = "";
        for (String providerId : providerOrder) {
            ChatProvider provider = providerFactory.createProvider(providerId);
            try {
                String reply = provider.generateReply(effectivePrompt);
                if (reply != null && !reply.isBlank() && !isProviderError(reply)) {
                    registerProviderHealth(providerId, IncidentSeverity.LOW, "Provider returned a valid response.", Map.of("provider", providerId));
                    return reply;
                }
                lastReason = reply == null ? "Empty provider response" : reply;
                registerProviderHealth(providerId, IncidentSeverity.MEDIUM, "Provider returned an error or empty reply.", Map.of("provider", providerId, "response", reply == null ? "null" : reply));
            } catch (Exception exception) {
                lastReason = exception.getMessage();
                registerProviderHealth(providerId, IncidentSeverity.HIGH, "Provider request failed: " + lastReason, Map.of("provider", providerId));
            }
        }

        HealthMonitorRegistry.instance().registerHealthSignal(new HealthSignal(
                "provider-failover",
                FailureType.PROVIDER,
                IncidentSeverity.CRITICAL,
                "All configured AI providers failed or returned degraded responses.",
                Instant.now(),
                Map.of("lastFailureReason", lastReason)));

        return "AI provider failover attempted; all configured providers are currently unavailable. "
                + "Check your API keys and network connectivity.";
    }

    private boolean isProviderError(String response) {
        if (response == null || response.isBlank()) {
            return true;
        }
        String lower = response.toLowerCase();
        return lower.startsWith("missing api key")
                || lower.startsWith("invalid provider endpoint")
                || lower.startsWith("unable to reach")
                || lower.contains("request failed with http")
                || lower.startsWith("i could not generate a response")
                || lower.contains("failed to fetch");
    }

    private void registerProviderHealth(String providerId, IncidentSeverity severity, String message, Map<String, String> metadata) {
        HealthMonitorRegistry.instance().registerHealthSignal(new HealthSignal(
                "provider:" + providerId,
                FailureType.PROVIDER,
                severity,
                message,
                Instant.now(),
                metadata));
    }
}
