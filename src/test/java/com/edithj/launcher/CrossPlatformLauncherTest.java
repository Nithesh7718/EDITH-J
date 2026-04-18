package com.edithj.launcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class CrossPlatformLauncherTest {

    @Test
    void isUrl_acceptsMailtoScheme() throws Exception {
        CrossPlatformLauncher launcher = new CrossPlatformLauncher();
        Method method = CrossPlatformLauncher.class.getDeclaredMethod("isUrl", String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(launcher, "mailto:test@example.com");

        assertTrue(result);
    }

    @Test
    void isUrl_rejectsPlainAppNames() throws Exception {
        CrossPlatformLauncher launcher = new CrossPlatformLauncher();
        Method method = CrossPlatformLauncher.class.getDeclaredMethod("isUrl", String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(launcher, "notepad");

        assertFalse(result);
    }
}
