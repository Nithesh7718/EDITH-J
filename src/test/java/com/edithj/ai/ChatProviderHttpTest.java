package com.edithj.ai;

import java.lang.reflect.Constructor;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edithj.config.EnvConfig;

class ChatProviderHttpTest {

    @Test
    void groqProvider_parsesOpenAiStyleResponse() throws Exception {
        HttpClient client = mockClientWithBody("{\"choices\":[{\"message\":{\"content\":\"hello from groq\"}}]}");
        GroqChatProvider provider = new GroqChatProvider(configWithKeys(), client);

        String response = provider.generateReply("hello");

        assertEquals("hello from groq", response);
    }

    @Test
    void openAiProvider_parsesOpenAiStyleResponse() throws Exception {
        HttpClient client = mockClientWithBody("{\"choices\":[{\"message\":{\"content\":\"hello from openai\"}}]}");
        OpenAIChatProvider provider = new OpenAIChatProvider(configWithKeys(), client);

        String response = provider.generateReply("hello");

        assertEquals("hello from openai", response);
    }

    @Test
    void sarvamProvider_parsesOpenAiStyleResponse() throws Exception {
        HttpClient client = mockClientWithBody("{\"choices\":[{\"message\":{\"content\":\"hello from sarvam\"}}]}");
        SarvamChatProvider provider = new SarvamChatProvider(configWithKeys(), client);

        String response = provider.generateReply("hello");

        assertEquals("hello from sarvam", response);
    }

    @Test
    void geminiProvider_parsesGeminiResponse() throws Exception {
        HttpClient client = mockClientWithBody("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"hello from gemini\"}]}}]}");
        GeminiChatProvider provider = new GeminiChatProvider(configWithKeys(), client);

        String response = provider.generateReply("hello");

        assertEquals("hello from gemini", response);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private HttpClient mockClientWithBody(String body) throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn((HttpResponse) response);
        return client;
    }

    private AiConfig configWithKeys() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("edith.ai.groq.apiKey", "groq-key");
        properties.setProperty("edith.ai.gemini.apiKey", "gemini-key");
        properties.setProperty("edith.ai.openai.apiKey", "openai-key");
        properties.setProperty("edith.ai.sarvam.apiKey", "sarvam-key");

        Constructor<EnvConfig> constructor = EnvConfig.class.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        EnvConfig envConfig = constructor.newInstance(Map.of());
        return new AiConfig(envConfig, properties);
    }
}
