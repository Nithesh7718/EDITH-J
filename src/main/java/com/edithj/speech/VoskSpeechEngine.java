package com.edithj.speech;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.vosk.Model;
import org.vosk.Recognizer;

public final class VoskSpeechEngine implements SpeechRecognizer.TranscriptionEngine {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final AudioFormat TARGET_AUDIO_FORMAT = new AudioFormat(16_000.0f, 16, 1, true, false);

    private final Path modelPath;
    private final Model model;
    private final boolean available;
    private final String unavailableReason;

    private VoskSpeechEngine(Path modelPath, Model model, boolean available, String unavailableReason) {
        this.modelPath = modelPath;
        this.model = model;
        this.available = available;
        this.unavailableReason = unavailableReason == null ? "" : unavailableReason.trim();
    }

    public static VoskSpeechEngine loadDefault() {
        return load(VoskSpeechConfig.resolveModelPath());
    }

    public static VoskSpeechEngine load(Path modelPath) {
        Path normalizedPath = modelPath == null ? VoskSpeechConfig.resolveModelPath() : modelPath.toAbsolutePath().normalize();

        if (!Files.isDirectory(normalizedPath)) {
            return unavailable(normalizedPath, "Vosk model folder was not found at " + normalizedPath);
        }

        if (!VoskModelHolder.isAvailable()) {
            return unavailable(normalizedPath, "Vosk model failed to load or is not present.");
        }
        return new VoskSpeechEngine(normalizedPath, VoskModelHolder.get(), true, "");
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public String unavailableReason() {
        if (!available) {
            if (!unavailableReason.isBlank()) {
                return unavailableReason;
            }
            return "Vosk model folder is unavailable at " + modelPath;
        }

        return "";
    }

    @Override
    public String transcribeWav(byte[] wavAudio) {
        if (!available) {
            throw new IllegalStateException(unavailableReason.isBlank()
                    ? "Vosk speech engine is unavailable"
                    : unavailableReason);
        }

        if (wavAudio == null || wavAudio.length == 0) {
            throw new NoSpeechRecognizedException("No speech recognized");
        }

        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavAudio)); AudioInputStream pcmStream = convertToRecognizerFormat(sourceStream); Recognizer recognizer = new Recognizer(model, TARGET_AUDIO_FORMAT.getSampleRate())) {

            byte[] buffer = new byte[4_096];
            int read;
            while ((read = pcmStream.read(buffer)) != -1) {
                if (read > 0) {
                    recognizer.acceptWaveForm(buffer, read);
                }
            }

            String finalJson = recognizer.getFinalResult();
            String transcript = extractTranscript(finalJson);
            if (transcript.isBlank()) {
                throw new NoSpeechRecognizedException("No speech recognized");
            }

            return transcript.trim();
        } catch (NoSpeechRecognizedException exception) {
            throw exception;
        } catch (UnsupportedAudioFileException | IOException exception) {
            throw new IllegalStateException("Unable to decode recorded audio", exception);
        }
    }

    private static VoskSpeechEngine unavailable(Path modelPath, String reason) {
        return new VoskSpeechEngine(modelPath, null, false, reason);
    }

    private AudioInputStream convertToRecognizerFormat(AudioInputStream sourceStream) throws UnsupportedAudioFileException {
        AudioFormat sourceFormat = sourceStream.getFormat();
        if (isRecognizerFormat(sourceFormat)) {
            return sourceStream;
        }

        if (!AudioSystem.isConversionSupported(TARGET_AUDIO_FORMAT, sourceFormat)) {
            throw new UnsupportedAudioFileException("Unsupported audio format for Vosk: " + sourceFormat);
        }

        return AudioSystem.getAudioInputStream(TARGET_AUDIO_FORMAT, sourceStream);
    }

    private boolean isRecognizerFormat(AudioFormat format) {
        return format != null
                && format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
                && format.getSampleRate() == TARGET_AUDIO_FORMAT.getSampleRate()
                && format.getSampleSizeInBits() == TARGET_AUDIO_FORMAT.getSampleSizeInBits()
                && format.getChannels() == TARGET_AUDIO_FORMAT.getChannels()
                && !format.isBigEndian();
    }

    private String extractTranscript(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            return root.path("text").asText("");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse Vosk transcription result", exception);
        }
    }
}
