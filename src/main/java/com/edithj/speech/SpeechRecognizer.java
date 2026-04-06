package com.edithj.speech;

import java.util.Optional;

public class SpeechRecognizer {

    @FunctionalInterface
    public interface TranscriptionEngine {

        String transcribeWav(byte[] wavAudio);
    }

    public record RecognitionResult(String transcript, byte[] wavAudio, boolean usedTypedFallback) {

    }

    private final AudioCapture audioCapture;
    private final TypedFallbackService typedFallbackService;
    private final TranscriptionEngine transcriptionEngine;

    public SpeechRecognizer() {
        this(new AudioCapture(), new TypedFallbackService(), null);
    }

    public SpeechRecognizer(AudioCapture audioCapture,
            TypedFallbackService typedFallbackService,
            TranscriptionEngine transcriptionEngine) {
        this.audioCapture = audioCapture;
        this.typedFallbackService = typedFallbackService;
        this.transcriptionEngine = transcriptionEngine;
    }

    public void startListening() {
        audioCapture.startRecording();
    }

    public RecognitionResult stopListeningAndRecognize() {
        byte[] wavAudio = audioCapture.stopRecording();

        String transcript = transcribe(wavAudio);
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

    private String transcribe(byte[] wavAudio) {
        if (transcriptionEngine == null || wavAudio == null || wavAudio.length == 0) {
            return "";
        }

        try {
            String text = transcriptionEngine.transcribeWav(wavAudio);
            return text == null ? "" : text.trim();
        } catch (RuntimeException exception) {
            return "";
        }
    }
}
