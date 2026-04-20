package com.edithj.speech;

import java.util.Optional;

public class SpeechRecognizer {

    @FunctionalInterface
    public interface TranscriptionEngine {

        String transcribeWav(byte[] wavAudio);

        default boolean isAvailable() {
            return true;
        }
    }

    public record RecognitionResult(String transcript, byte[] wavAudio, boolean usedTypedFallback) {

    }

    private final AudioCapture audioCapture;
    private final TypedFallbackService typedFallbackService;
    private final TranscriptionEngine transcriptionEngine;

    public SpeechRecognizer() {
        this(new AudioCapture(), new TypedFallbackService(), VoskSpeechEngine.loadDefault());
    }

    public SpeechRecognizer(AudioCapture audioCapture,
            TypedFallbackService typedFallbackService,
            TranscriptionEngine transcriptionEngine) {
        this.audioCapture = audioCapture;
        this.typedFallbackService = typedFallbackService;
        this.transcriptionEngine = transcriptionEngine;
    }

    public void startListening() {
        if (!isAvailable()) {
            throw new IllegalStateException("Speech recognition is unavailable");
        }
        audioCapture.startRecording();
    }

    public RecognitionResult stopListeningAndRecognize() {
        byte[] wavAudio = audioCapture.stopRecording();

        String transcript;
        try {
            transcript = transcribe(wavAudio);
        } catch (NoSpeechRecognizedException exception) {
            return new RecognitionResult("", wavAudio, false);
        }
        boolean usedTypedFallback = false;

        if (transcript.isBlank()) {
            Optional<String> fallbackText = typedFallbackService.requestTypedInput(
                    "Voice capture completed, but transcription is unavailable. Type your request instead:"
            );
            if (fallbackText.isPresent()) {
                transcript = fallbackText.get();
                usedTypedFallback = true;
            }
        }

        return new RecognitionResult(transcript.trim(), wavAudio, usedTypedFallback);
    }

    public TypedFallbackService typedFallbackService() {
        return typedFallbackService;
    }

    public boolean isAvailable() {
        return transcriptionEngine != null && transcriptionEngine.isAvailable();
    }

    private String transcribe(byte[] wavAudio) {
        if (transcriptionEngine == null || wavAudio == null || wavAudio.length == 0) {
            return "";
        }

        try {
            String text = transcriptionEngine.transcribeWav(wavAudio);
            return text == null ? "" : text.trim();
        } catch (NoSpeechRecognizedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            return "";
        }
    }
}
