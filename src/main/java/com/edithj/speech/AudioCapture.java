package com.edithj.speech;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioCapture {

    private static final int BUFFER_SIZE = 4_096;

    private final AudioFormat format;

    private volatile boolean recording;
    private volatile TargetDataLine microphone;
    private volatile Thread captureThread;

    private ByteArrayOutputStream pcmBuffer;
    private byte[] lastRecordingWav = new byte[0];

    public AudioCapture() {
        this(new AudioFormat(16_000.0f, 16, 1, true, false));
    }

    public AudioCapture(AudioFormat format) {
        this.format = format;
    }

    public synchronized void startRecording() {
        if (recording) {
            return;
        }

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            microphone = line;
            pcmBuffer = new ByteArrayOutputStream();
            recording = true;

            Thread thread = new Thread(this::captureLoop, "edithj-audio-capture");
            thread.setDaemon(true);
            captureThread = thread;
            thread.start();
        } catch (javax.sound.sampled.LineUnavailableException exception) {
            recording = false;
            microphone = null;
            throw new IllegalStateException("Unable to start audio recording", exception);
        }
    }

    public synchronized byte[] stopRecording() {
        if (!recording) {
            return lastRecordingWav.clone();
        }

        recording = false;

        TargetDataLine line = microphone;
        if (line != null) {
            line.stop();
            line.close();
        }

        Thread thread = captureThread;
        if (thread != null) {
            try {
                thread.join(1_500);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        microphone = null;
        captureThread = null;

        byte[] pcmBytes = pcmBuffer != null ? pcmBuffer.toByteArray() : new byte[0];
        pcmBuffer = null;

        if (pcmBytes.length == 0) {
            lastRecordingWav = new byte[0];
            return new byte[0];
        }

        lastRecordingWav = toWav(pcmBytes);
        return lastRecordingWav.clone();
    }

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized byte[] getLastRecordingWav() {
        return lastRecordingWav.clone();
    }

    private void captureLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (recording) {
            TargetDataLine line = microphone;
            if (line == null) {
                break;
            }

            int read = line.read(buffer, 0, buffer.length);
            if (read > 0 && pcmBuffer != null) {
                pcmBuffer.write(buffer, 0, read);
            }
        }
    }

    private byte[] toWav(byte[] pcmBytes) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(pcmBytes); AudioInputStream audioStream = new AudioInputStream(input, format, pcmBytes.length / format.getFrameSize()); ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encode recorded audio as WAV", exception);
        }
    }
}
