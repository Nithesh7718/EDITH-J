package com.edithj.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.edithj.config.EnvConfig;
import com.edithj.config.ModelConfig;

class ConfigLoadingTest {

    @Test
    void envConfig_handlesBlankAndMissingKeys() {
        EnvConfig envConfig = EnvConfig.system();

        assertTrue(envConfig.get("   ").isEmpty());
        assertFalse(envConfig.isPresent("definitely_missing_key_12345"));
        assertEquals("fallback", envConfig.getOrDefault("definitely_missing_key_12345", "fallback"));
    }

    @Test
    void modelConfig_load_usesPropertiesAndNormalizesValues() throws Exception {
        EnvConfig envConfig = createEnvConfig(Map.of(
                "GROQ_API_KEY", "  test-key  ",
                "GROQ_BASE_URL", " https://api.groq.com/openai/v1/ ",
                "GROQ_TIMEOUT_SECONDS", "invalid",
                "GROQ_TEMPERATURE", "9.99",
                "GROQ_MODEL", " custom-model "));

        Properties properties = new Properties();
        properties.setProperty("groq.timeout-seconds", "45");

        ModelConfig modelConfig = ModelConfig.load(envConfig, properties);

        assertTrue(modelConfig.isConfigured());
        assertEquals("test-key", modelConfig.apiKey());
        assertEquals("https://api.groq.com/openai/v1", modelConfig.baseUrl());
        assertEquals("custom-model", modelConfig.model());
        assertEquals(Duration.ofSeconds(30), modelConfig.requestTimeout());
        assertEquals(2.0d, modelConfig.temperature());
    }

    private EnvConfig createEnvConfig(Map<String, String> values) throws Exception {
        Constructor<EnvConfig> constructor = EnvConfig.class.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        return constructor.newInstance(values);
    }
}
