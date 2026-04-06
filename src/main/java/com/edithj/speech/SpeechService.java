package com.edithj.speech;

public class SpeechService {

    public record CapturedInput(String text, byte[] wavAudio, boolean usedTypedFallback) {

    }

    private final SpeechRecognizer speechRecognizer;

    public SpeechService() {
        this(new SpeechRecognizer());
    }

    public SpeechService(SpeechRecognizer speechRecognizer) {
        this.speechRecognizer = speechRecognizer;
    }

    public void startListening() {
        speechRecognizer.startListening();
    }

    public CapturedInput stopListening() {
        SpeechRecognizer.RecognitionResult result = speechRecognizer.stopListeningAndRecognize();
        return new CapturedInput(result.transcript(), result.wavAudio(), result.usedTypedFallback());
    }

    public TypedFallbackService typedFallbackService() {
        return speechRecognizer.typedFallbackService();
    }
}
