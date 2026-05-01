package com.edithj.ai;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.config.EnvConfig;

class ProviderFactoryTest {

    @Test
    void createProvider_selectsConfiguredProvider() throws Exception {
        assertInstanceOf(GroqChatProvider.class, factoryFor("groq").createProvider());
        assertInstanceOf(GeminiChatProvider.class, factoryFor("gemini").createProvider());
        assertInstanceOf(OpenAIChatProvider.class, factoryFor("openai").createProvider());
        assertInstanceOf(SarvamChatProvider.class, factoryFor("sarvam").createProvider());
    }

    @Test
    void createProvider_returnsMissingProviderWhenKeyUnavailable() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("edith.ai.provider", "gemini");

        AiConfig config = new AiConfig(envConfig(Map.of()), properties);
        ProviderFactory factory = new ProviderFactory(config);

        ChatProvider provider = factory.createProvider();
        assertInstanceOf(MissingApiKeyChatProvider.class, provider);
        assertTrue(provider.generateReply("hi").contains("GEMINI_API_KEY"));
    }

    @Test
    void createProvider_allowsExplicitOverride() throws Exception {
        ProviderFactory factory = factoryFor("groq");

        ChatProvider provider = factory.createProvider("openai");

        assertInstanceOf(OpenAIChatProvider.class, provider);
    }

    private ProviderFactory factoryFor(String configuredProvider) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("edith.ai.provider", configuredProvider);
        properties.setProperty("edith.ai.groq.apiKey", "test-groq");
        properties.setProperty("edith.ai.gemini.apiKey", "test-gemini");
        properties.setProperty("edith.ai.openai.apiKey", "test-openai");
        properties.setProperty("edith.ai.sarvam.apiKey", "test-sarvam");

        AiConfig config = new AiConfig(envConfig(Map.of()), properties);
        return new ProviderFactory(config);
    }

    private EnvConfig envConfig(Map<String, String> values) throws Exception {
        Constructor<EnvConfig> constructor = EnvConfig.class.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        return constructor.newInstance(values);
    }
}
