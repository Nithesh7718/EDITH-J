package com.edithj.ui.session;

import java.util.Objects;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import com.edithj.speech.SpeechRecognizer;
import com.edithj.speech.NoSpeechRecognizedException;

import javafx.application.Platform;

public final class UiSpeechService {

    private static final long SINGLE_UTTERANCE_TIMEOUT_MS = 5_500;

    private final SpeechRecognizer recognizer;
    private final Object lock = new Object();

    private volatile boolean listening;
    private volatile boolean available;
    private long activeSessionId;

    public UiSpeechService(SpeechRecognizer recognizer) {
        this.recognizer = Objects.requireNonNull(recognizer, "recognizer");
        this.available = this.recognizer.isAvailable() && probeMicrophoneAvailability();
    }

    public void startListening(Consumer<String> onResult, Consumer<Throwable> onError) {
        Objects.requireNonNull(onResult, "onResult");
        Objects.requireNonNull(onError, "onError");

        if (!available) {
            runOnUiThread(() -> onError.accept(new IllegalStateException("Microphone not available")));
            return;
        }

        final long sessionId;
        synchronized (lock) {
            if (listening) {
                return;
            }
            listening = true;
            activeSessionId++;
            sessionId = activeSessionId;
        }

        try {
            recognizer.startListening();
        } catch (RuntimeException exception) {
            synchronized (lock) {
                listening = false;
            }
            available = probeMicrophoneAvailability();
            runOnUiThread(() -> onError.accept(exception));
            return;
        }

        Thread recognizeThread = new Thread(
                () -> runRecognitionAfterSingleUtterance(sessionId, onResult, onError),
                "edithj-voice-once-" + sessionId
        );
        recognizeThread.setDaemon(true);
        recognizeThread.start();
    }

    public void stopListening() {
        synchronized (lock) {
            if (!listening) {
                return;
            }
            listening = false;
            activeSessionId++;
        }

        try {
            recognizer.stopListeningAndRecognize();
        } catch (RuntimeException exception) {
            available = probeMicrophoneAvailability();
        }
    }

    public boolean isListening() {
        return listening;
    }

    public boolean isAvailable() {
        return available;
    }

    private void runRecognitionAfterSingleUtterance(long sessionId,
            Consumer<String> onResult,
            Consumer<Throwable> onError) {
        try {
            Thread.sleep(SINGLE_UTTERANCE_TIMEOUT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        }

        synchronized (lock) {
            if (!listening || sessionId != activeSessionId) {
                return;
            }
            listening = false;
        }

        try {
            SpeechRecognizer.RecognitionResult result = recognizer.stopListeningAndRecognize();
            String transcript = result == null ? "" : result.transcript();
            if (transcript == null || transcript.isBlank()) {
                runOnUiThread(() -> onError.accept(new NoSpeechRecognizedException("No speech recognized")));
                return;
            }
            runOnUiThread(() -> onResult.accept(transcript.trim()));
        } catch (RuntimeException exception) {
            available = probeMicrophoneAvailability();
            runOnUiThread(() -> onError.accept(exception));
        }
    }

    private boolean probeMicrophoneAvailability() {
        AudioFormat format = new AudioFormat(16_000.0f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            return false;
        }

        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private void runOnUiThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
            return;
        }
        Platform.runLater(runnable);
    }
}
