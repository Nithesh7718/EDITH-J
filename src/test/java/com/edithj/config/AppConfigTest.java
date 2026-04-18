package com.edithj.config;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void desktopToggles_defaultToEnabledWhenMissing() throws Exception {
        AppConfig appConfig = createAppConfig(new Properties());

        assertTrue(appConfig.isDesktopFileOpenEnabled());
        assertTrue(appConfig.isDesktopClipboardWriteEnabled());
    }

    @Test
    void desktopToggles_parseFalseValues() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("edith.desktop.fileOpenEnabled", "false");
        properties.setProperty("edith.desktop.clipboardWriteEnabled", "false");

        AppConfig appConfig = createAppConfig(properties);

        assertFalse(appConfig.isDesktopFileOpenEnabled());
        assertFalse(appConfig.isDesktopClipboardWriteEnabled());
    }

    private AppConfig createAppConfig(Properties properties) throws Exception {
        Constructor<EnvConfig> envConfigConstructor = EnvConfig.class.getDeclaredConstructor(Map.class);
        envConfigConstructor.setAccessible(true);
        EnvConfig envConfig = envConfigConstructor.newInstance(Map.of());

        Constructor<AppConfig> appConfigConstructor = AppConfig.class.getDeclaredConstructor(EnvConfig.class,
                Properties.class);
        appConfigConstructor.setAccessible(true);
        return appConfigConstructor.newInstance(envConfig, properties);
    }
}
