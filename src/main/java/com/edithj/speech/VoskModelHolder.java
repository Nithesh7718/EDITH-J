package com.edithj.speech;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vosk.Model;

/**
 * FIX: Singleton holder for the Vosk acoustic model.
 * The Model object (~11 MB native memory) is loaded exactly ONCE per process.
 * All speech engine instances share this single model reference.
 */
public final class VoskModelHolder {

    private static final Logger LOG = Logger.getLogger(VoskModelHolder.class.getName());

    /**
     * The single shared model instance. {@code null} when the model directory
     * does not exist or failed to load (Vosk will be unavailable but the app
     * will not crash).
     */
    private static final Model MODEL = loadOnce();

    private VoskModelHolder() {
    }

    /**
     * Returns the shared Vosk {@link Model}, or {@code null} if the model
     * could not be loaded. Callers must check {@link #isAvailable()} first.
     */
    public static Model get() {
        return MODEL;
    }

    /**
     * Returns {@code true} when the model was loaded successfully and is ready
     * to be used for transcription.
     */
    public static boolean isAvailable() {
        return MODEL != null;
    }

    // ── private ───────────────────────────────────────────────────────────────

    private static Model loadOnce() {
        Path modelPath = VoskSpeechConfig.resolveModelPath();

        if (!Files.isDirectory(modelPath)) {
            LOG.warning("Vosk model directory not found at " + modelPath
                    + " — speech recognition will be unavailable.");
            return null;
        }

        try {
            LOG.info("Loading Vosk model from " + modelPath + " (one-time startup load)");
            Model model = new Model(modelPath.toString());
            LOG.info("Vosk model loaded successfully.");
            return model;
        } catch (Exception | UnsatisfiedLinkError ex) {
            LOG.log(Level.WARNING, "Failed to load Vosk model from " + modelPath
                    + " — speech recognition will be unavailable.", ex);
            return null;
        }
    }
}
