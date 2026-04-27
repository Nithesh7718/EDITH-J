package com.edithj.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edithj.ai.ChatProvider;
import com.edithj.ai.ProviderFactory;
import com.edithj.integration.llm.ProviderBackedLlmClient;

class ProviderBackedLlmClientTest {

    @Test
    void generateReply_supportsNaturalLanguageProviderOverride() {
        ProviderFactory providerFactory = mock(ProviderFactory.class);
        ChatProvider provider = mock(ChatProvider.class);
        when(providerFactory.createProvider(eq("gemini"))).thenReturn(provider);
        when(provider.generateReply("summarize this note")).thenReturn("ok");

        ProviderBackedLlmClient client = new ProviderBackedLlmClient(providerFactory);

        String reply = client.generateReply("use gemini for this answer: summarize this note");

        assertEquals("ok", reply);
        verify(providerFactory).createProvider("gemini");
        verify(provider).generateReply("summarize this note");
    }
}
