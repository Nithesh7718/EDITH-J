package com.edithj.speech;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpeechRecognizerTest {

    @Test
    void stopListeningAndRecognize_usesTranscriptionWhenAvailable() {
        byte[] audio = new byte[]{1, 2, 3};
        FakeAudioCapture audioCapture = new FakeAudioCapture(audio);
        TypedFallbackService typedFallbackService = new TypedFallbackService();

        SpeechRecognizer recognizer = new SpeechRecognizer(audioCapture, typedFallbackService, wav -> "  hello from voice  ");

        SpeechRecognizer.RecognitionResult result = recognizer.stopListeningAndRecognize();

        assertEquals("hello from voice", result.transcript());
        assertArrayEquals(audio, result.wavAudio());
        assertFalse(result.usedTypedFallback());
    }

    @Test
    void stopListeningAndRecognize_fallsBackToTypedInputWhenTranscriptionMissing() {
        byte[] audio = new byte[]{4, 5, 6};
        FakeAudioCapture audioCapture = new FakeAudioCapture(audio);
        TypedFallbackService typedFallbackService = new TypedFallbackService();
        typedFallbackService.setTypedInputProvider(prompt -> "typed fallback text");

        SpeechRecognizer recognizer = new SpeechRecognizer(audioCapture, typedFallbackService, wav -> " ");

        SpeechRecognizer.RecognitionResult result = recognizer.stopListeningAndRecognize();

        assertEquals("typed fallback text", result.transcript());
        assertTrue(result.usedTypedFallback());
    }

    @Test
    void stopListeningAndRecognize_returnsEmptyWhenNoFallbackProvided() {
        FakeAudioCapture audioCapture = new FakeAudioCapture(new byte[0]);
        TypedFallbackService typedFallbackService = new TypedFallbackService();

        SpeechRecognizer recognizer = new SpeechRecognizer(audioCapture, typedFallbackService, wav -> "");

        SpeechRecognizer.RecognitionResult result = recognizer.stopListeningAndRecognize();

        assertEquals("", result.transcript());
        assertFalse(result.usedTypedFallback());
    }

    @Test
    void stopListeningAndRecognize_treatsNoSpeechExceptionAsSilentFailureWithoutFallback() {
        byte[] audio = new byte[]{7, 8, 9};
        FakeAudioCapture audioCapture = new FakeAudioCapture(audio);
        TypedFallbackService typedFallbackService = new TypedFallbackService();
        typedFallbackService.setTypedInputProvider(prompt -> "typed fallback text");

        SpeechRecognizer recognizer = new SpeechRecognizer(audioCapture, typedFallbackService,
                wav -> {
                    throw new NoSpeechRecognizedException("No speech recognized");
                });

        SpeechRecognizer.RecognitionResult result = recognizer.stopListeningAndRecognize();

        assertEquals("", result.transcript());
        assertArrayEquals(audio, result.wavAudio());
        assertFalse(result.usedTypedFallback());
    }

    @Test
    void isAvailable_reflectsTranscriptionEngineAvailability() {
        FakeAudioCapture audioCapture = new FakeAudioCapture(new byte[0]);
        TypedFallbackService typedFallbackService = new TypedFallbackService();

        SpeechRecognizer recognizer = new SpeechRecognizer(audioCapture, typedFallbackService, new SpeechRecognizer.TranscriptionEngine() {
            @Override
            public String transcribeWav(byte[] wavAudio) {
                return "";
            }

            @Override
            public boolean isAvailable() {
                return false;
            }
        });

        assertFalse(recognizer.isAvailable());
    }

    private static class FakeAudioCapture extends AudioCapture {

        private final byte[] audio;

        FakeAudioCapture(byte[] audio) {
            this.audio = audio == null ? new byte[0] : audio.clone();
        }

        @Override
        public synchronized void startRecording() {
            // No-op for tests.
        }

        @Override
        public synchronized byte[] stopRecording() {
            return audio.clone();
        }
    }
}
