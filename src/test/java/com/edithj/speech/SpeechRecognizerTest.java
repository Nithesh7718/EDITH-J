package com.edithj.speech;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class SpeechRecognizerTest {

    @Mock
    private AudioCapture audioCapture;

    @Mock
    private TypedFallbackService typedFallbackService;

    @Mock
    private SpeechRecognizer.TranscriptionEngine transcriptionEngine;

    private SpeechRecognizer recognizer;

    @BeforeEach
    void setUp() {
        recognizer = new SpeechRecognizer(audioCapture, typedFallbackService, transcriptionEngine);
    }

    @Test
    void stopListeningAndRecognize_usesTranscriptionWhenAvailable() {
        byte[] audio = new byte[] {1, 2, 3};
        when(audioCapture.stopRecording()).thenReturn(audio);
        when(transcriptionEngine.transcribeWav(audio)).thenReturn("  hello from voice  ");

        SpeechRecognizer.RecognitionResult result = recognizer.stopListeningAndRecognize();

        assertEquals("hello from voice", result.transcript());
        assertArrayEquals(audio, result.wavAudio());
        assertFalse(result.usedTypedFallback());
    }

    @Test
    void stopListeningAndRecognize_fallsBackToTypedInputWhenTranscriptionMissing() {
        byte[] audio = new byte[] {4, 5, 6};
        when(audioCapture.stopRecording()).thenReturn(audio);
        when(transcriptionEngine.transcribeWav(audio)).thenReturn(" ");
        when(typedFallbackService.requestTypedInput(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of("typed fallback text"));

        SpeechRecognizer.RecognitionResult result = recognizer.stopListeningAndRecognize();

        assertEquals("typed fallback text", result.transcript());
        assertTrue(result.usedTypedFallback());
        verify(typedFallbackService).requestTypedInput(org.mockito.ArgumentMatchers.contains("transcription is unavailable"));
    }

    @Test
    void stopListeningAndRecognize_returnsEmptyWhenNoFallbackProvided() {
        when(audioCapture.stopRecording()).thenReturn(new byte[0]);
        when(typedFallbackService.requestTypedInput(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        SpeechRecognizer.RecognitionResult result = recognizer.stopListeningAndRecognize();

        assertEquals("", result.transcript());
        assertFalse(result.usedTypedFallback());
    }
}
