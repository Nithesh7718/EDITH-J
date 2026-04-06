package com.edithj.speech;

import java.util.Optional;
import java.util.function.Function;

public class TypedFallbackService {

    private volatile Function<String, String> typedInputProvider;

    public void setTypedInputProvider(Function<String, String> typedInputProvider) {
        this.typedInputProvider = typedInputProvider;
    }

    public Optional<String> requestTypedInput(String prompt) {
        Function<String, String> provider = this.typedInputProvider;
        if (provider == null) {
            return Optional.empty();
        }

        String value = provider.apply(prompt == null ? "" : prompt.trim());
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value.trim());
    }
}
