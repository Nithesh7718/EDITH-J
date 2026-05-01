package com.edithj.launcher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AppLauncherServiceTest {

    @Test
    void launchApp_fallsBackToYoutubeWebWhenShortcutIsUnavailable() {
        CrossPlatformLauncher launcher = new CrossPlatformLauncher() {
            @Override
            public String launch(String target) {
                if ("youtube".equalsIgnoreCase(target)) {
                    return "Could not find or launch youtube.";
                }
                if ("https://www.youtube.com".equals(target)) {
                    return "Opening https://www.youtube.com.";
                }
                return super.launch(target);
            }
        };

        AppLauncherService service = new AppLauncherService(launcher);

        String response = service.launchApp("youtube");

        assertTrue(response.contains("www.youtube.com") || response.toLowerCase().contains("opening"));
    }
}
