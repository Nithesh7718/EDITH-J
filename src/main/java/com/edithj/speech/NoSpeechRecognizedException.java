package com.edithj.speech;

public final class NoSpeechRecognizedException extends RuntimeException {

    public NoSpeechRecognizedException(String message) {
        super(message == null || message.isBlank() ? "No speech recognized" : message.trim(), null, false, false);
    }
}
