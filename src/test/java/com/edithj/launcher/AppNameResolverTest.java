package com.edithj.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AppNameResolverTest {

    private final AppNameResolver resolver = new AppNameResolver();

    @ParameterizedTest
    @CsvSource({
            "whatsapp, whatsapp",
            "what's up, whatsapp",
            "whats up, whatsapp",
            "whtsapp, whatsapp",
            "whatsap, whatsapp",
            "watsapp, whatsapp",
            "yt, youtube",
            "you tube, youtube",
            "file explorer, explorer",
            "my files, explorer"
    })
    void resolveLaunchTarget_knownAliasesResolveToExpectedCanonicalTarget(String input, String expected) {
        assertEquals(expected, resolver.resolveLaunchTarget(input));
    }

    @ParameterizedTest
    @CsvSource({
            "calculator, calculator",
            "notepad, notepad",
            "spotify, spotify"
    })
    void resolveLaunchTarget_unknownAppsPassThroughNormalized(String input, String expected) {
        assertEquals(expected, resolver.resolveLaunchTarget(input));
    }
}
