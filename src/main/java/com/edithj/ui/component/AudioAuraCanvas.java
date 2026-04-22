package com.edithj.ui.component;

import com.edithj.ui.model.AssistantUiState;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

/**
 * A JavaFX {@link Canvas} that renders a FRIDAY-style circular audio aura.
 *
 * <p>The aura is composed of:
 * <ul>
 *   <li>A centre avatar glow circle with an EDITH logo.</li>
 *   <li>N radial "spike" bars whose height is modulated by {@link #audioLevelProperty()}.</li>
 *   <li>Two outer glow rings that pulse according to {@link #uiStateProperty()}.</li>
 * </ul>
 *
 * <p>CPU usage is kept low by drawing on an {@link javafx.animation.AnimationTimer}-driven
 * internal loop that only redraws when the canvas is visible.
 */
public final class AudioAuraCanvas extends Canvas {

    // ── Configuration constants ──────────────────────────────────────────────
    private static final int    BAR_COUNT        = 64;
    private static final double BAR_MIN_H        = 4.0;
    private static final double BAR_MAX_H        = 52.0;
    private static final double BAR_WIDTH        = 3.2;
    private static final double AVATAR_RADIUS    = 68.0;
    private static final double INNER_RING_GAP   = 14.0;   // gap between avatar edge and bars
    private static final Color  COLOR_CYAN       = Color.web("#1FD5F9");
    private static final Color  COLOR_CYAN_DIM   = Color.web("#1FD5F9", 0.18);
    private static final Color  COLOR_PURPLE     = Color.web("#7B61FF");
    private static final Color  COLOR_IDLE       = Color.web("#1FD5F9", 0.55);
    private static final Color  COLOR_LISTENING  = Color.web("#1FD5F9", 0.95);
    private static final Color  COLOR_PROCESSING = Color.web("#7B61FF", 0.80);
    private static final Color  COLOR_SPEAKING   = Color.web("#00FFA3", 0.90);

    // ── Observable properties ────────────────────────────────────────────────
    /** Current audio input level in [0, 1]. Bind to your microphone volume meter. */
    private final DoubleProperty audioLevel =
            new SimpleDoubleProperty(this, "audioLevel", 0.0);

    /** Current assistant UI state. Changes trigger animation reconfiguration. */
    private final ObjectProperty<AssistantUiState> uiState =
            new SimpleObjectProperty<>(this, "uiState", AssistantUiState.IDLE);

    // ── Internal animation ───────────────────────────────────────────────────
    /** Animated amplitude multiplier for the breathing / pulse effect (0–1 range). */
    private final DoubleProperty animAmplitude =
            new SimpleDoubleProperty(this, "animAmplitude", 0.0);

    /** Animated outer ring scale used for listening pulse expansion. */
    private final DoubleProperty outerRingScale =
            new SimpleDoubleProperty(this, "outerRingScale", 1.0);

    private Timeline stateTimeline;

    // ── Rendering loop ───────────────────────────────────────────────────────
    private final javafx.animation.AnimationTimer renderLoop;

    // ── Pre-computed bar angles ──────────────────────────────────────────────
    private final double[] barAngles = new double[BAR_COUNT];
    private final double[] barPhase  = new double[BAR_COUNT];  // random phase offset per bar

    // ────────────────────────────────────────────────────────────────────────
    public AudioAuraCanvas(double size) {
        super(size, size);
        precomputeBarAngles();
        renderLoop = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                draw(now);
            }
        };
        renderLoop.start();

        // React to state changes
        uiState.addListener((obs, oldState, newState) -> applyStateAnimation(newState));
        applyStateAnimation(AssistantUiState.IDLE);
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public DoubleProperty audioLevelProperty()             { return audioLevel; }
    public double         getAudioLevel()                  { return audioLevel.get(); }
    public void           setAudioLevel(double v)          { audioLevel.set(Math.max(0, Math.min(1, v))); }

    public ObjectProperty<AssistantUiState> uiStateProperty() { return uiState; }
    public AssistantUiState getUiState()                       { return uiState.get(); }
    public void             setUiState(AssistantUiState s)     { uiState.set(s); }

    // ── Pre-computation ──────────────────────────────────────────────────────
    private void precomputeBarAngles() {
        for (int i = 0; i < BAR_COUNT; i++) {
            barAngles[i] = (2 * Math.PI * i) / BAR_COUNT - Math.PI / 2;
            barPhase[i]  = Math.random() * Math.PI * 2;
        }
    }

    // ── State-driven animation ────────────────────────────────────────────────
    private void applyStateAnimation(AssistantUiState state) {
        if (stateTimeline != null) {
            stateTimeline.stop();
        }

        stateTimeline = switch (state) {
            case IDLE -> buildIdleBreath();
            case LISTENING -> buildListeningPulse();
            case PROCESSING -> buildProcessingSpin();
            case SPEAKING -> buildSpeakingWave();
        };
        stateTimeline.setCycleCount(Animation.INDEFINITE);
        stateTimeline.play();
    }

    private Timeline buildIdleBreath() {
        // Slow breathing: 0.15 → 0.35 over ~2.8 s, auto-reversed
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO,       new KeyValue(animAmplitude, 0.12)),
            new KeyFrame(Duration.seconds(2.8), new KeyValue(animAmplitude, 0.32))
        );
        t.setAutoReverse(true);
        return t;
    }

    private Timeline buildListeningPulse() {
        // Fast energetic pulse + expanding outer ring
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(animAmplitude,  0.20),
                new KeyValue(outerRingScale, 1.0)),
            new KeyFrame(Duration.millis(500),
                new KeyValue(animAmplitude,  0.70),
                new KeyValue(outerRingScale, 1.08)),
            new KeyFrame(Duration.millis(1000),
                new KeyValue(animAmplitude,  0.20),
                new KeyValue(outerRingScale, 1.0))
        );
        t.setAutoReverse(false);
        return t;
    }

    private Timeline buildProcessingSpin() {
        // Medium amplitude, subtle orbit rotation feel (achieved via phase offset animation)
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO,       new KeyValue(animAmplitude, 0.30)),
            new KeyFrame(Duration.millis(800), new KeyValue(animAmplitude, 0.55)),
            new KeyFrame(Duration.millis(1600), new KeyValue(animAmplitude, 0.30))
        );
        t.setAutoReverse(false);
        return t;
    }

    private Timeline buildSpeakingWave() {
        // Strong amplitude modulation
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO,       new KeyValue(animAmplitude, 0.55)),
            new KeyFrame(Duration.millis(300), new KeyValue(animAmplitude, 0.90)),
            new KeyFrame(Duration.millis(600), new KeyValue(animAmplitude, 0.55))
        );
        t.setAutoReverse(false);
        return t;
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    private void draw(long nowNanos) {
        double w      = getWidth();
        double h      = getHeight();
        double cx     = w / 2.0;
        double cy     = h / 2.0;
        double t      = nowNanos / 1_000_000_000.0; // seconds

        GraphicsContext gc = getGraphicsContext2D();

        // Clear
        gc.clearRect(0, 0, w, h);

        AssistantUiState state = uiState.get();
        double level    = audioLevel.get();
        double anim     = animAmplitude.get();
        double ringScale = outerRingScale.get();

        // Combined effective amplitude — audio level overrides animation when strong
        double effective = Math.min(1.0, anim + level * 0.6);

        Color barColor = switch (state) {
            case IDLE       -> COLOR_IDLE;
            case LISTENING  -> COLOR_LISTENING;
            case PROCESSING -> COLOR_PROCESSING;
            case SPEAKING   -> COLOR_SPEAKING;
        };

        // ── Outer glow rings ──────────────────────────────────────────────────
        double rComponent = barColor.getRed();
        double gComponent = barColor.getGreen();
        double bComponent = barColor.getBlue();
        double baseAlpha = barColor.getOpacity();

        double ringR1 = AVATAR_RADIUS + INNER_RING_GAP + BAR_MAX_H * 0.5 * ringScale;
        drawGlowRing(gc, cx, cy, ringR1, Color.color(rComponent, gComponent, bComponent, baseAlpha * (0.08 + effective * 0.12)), 1.5);
        double ringR2 = ringR1 + 8 * ringScale;
        drawGlowRing(gc, cx, cy, ringR2, Color.color(rComponent, gComponent, bComponent, baseAlpha * (0.05 + effective * 0.06)), 1.0);

        // ── Aura bars ─────────────────────────────────────────────────────────
        double innerR = AVATAR_RADIUS + INNER_RING_GAP;
        for (int i = 0; i < BAR_COUNT; i++) {
            double angle  = barAngles[i];
            // Per-bar height: animated wave + audio level modulation
            double wave   = Math.sin(t * 3.0 + barPhase[i]) * 0.5 + 0.5;
            double barH   = BAR_MIN_H + (BAR_MAX_H - BAR_MIN_H) * (effective * 0.7 + wave * effective * 0.3);

            double x1 = cx + innerR * Math.cos(angle);
            double y1 = cy + innerR * Math.sin(angle);
            double x2 = cx + (innerR + barH) * Math.cos(angle);
            double y2 = cy + (innerR + barH) * Math.sin(angle);

            // Alpha varies with bar height for additional depth
            double alpha = 0.35 + 0.65 * (barH / BAR_MAX_H);
            gc.setStroke(Color.color(rComponent, gComponent, bComponent, baseAlpha * alpha));
            gc.setLineWidth(BAR_WIDTH);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.strokeLine(x1, y1, x2, y2);
        }

        // ── Avatar circle ─────────────────────────────────────────────────────
        drawAvatar(gc, cx, cy, state, effective);
    }

    private void drawGlowRing(GraphicsContext gc, double cx, double cy,
                               double r, Color color, double lineWidth) {
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawAvatar(GraphicsContext gc, double cx, double cy,
                             AssistantUiState state, double effective) {
        // Outer soft glow
        double glowR = AVATAR_RADIUS + 10 + effective * 8;
        RadialGradient glowPaint = new RadialGradient(
            0, 0, cx, cy, glowR,
            false, CycleMethod.NO_CYCLE,
            new Stop(0.0, switch (state) {
                case IDLE       -> Color.web("#1FD5F9", 0.20);
                case LISTENING  -> Color.web("#1FD5F9", 0.45);
                case PROCESSING -> Color.web("#7B61FF", 0.35);
                case SPEAKING   -> Color.web("#00FFA3", 0.40);
            }),
            new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(glowPaint);
        gc.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);

        // Main avatar body — dark with subtle radial gradient
        RadialGradient bodyPaint = new RadialGradient(
            0, 0, cx, cy, AVATAR_RADIUS,
            false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#0D1B2E")),
            new Stop(1.0, Color.web("#050910"))
        );
        gc.setFill(bodyPaint);
        gc.fillOval(cx - AVATAR_RADIUS, cy - AVATAR_RADIUS,
                    AVATAR_RADIUS * 2, AVATAR_RADIUS * 2);

        // Avatar border ring
        Color borderColor = switch (state) {
            case IDLE       -> COLOR_CYAN_DIM;
            case LISTENING  -> COLOR_CYAN;
            case PROCESSING -> COLOR_PURPLE;
            case SPEAKING   -> Color.web("#00FFA3");
        };
        gc.setStroke(borderColor);
        gc.setLineWidth(2.0);
        gc.strokeOval(cx - AVATAR_RADIUS, cy - AVATAR_RADIUS,
                      AVATAR_RADIUS * 2, AVATAR_RADIUS * 2);

        // "E" text logo
        gc.setFill(Color.color(COLOR_CYAN.getRed(), COLOR_CYAN.getGreen(), COLOR_CYAN.getBlue(), COLOR_CYAN.getOpacity() * (0.90 + effective * 0.10)));
        gc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 34));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText("E", cx, cy);

        // State sub-label
        gc.setFill(Color.color(COLOR_CYAN.getRed(), COLOR_CYAN.getGreen(), COLOR_CYAN.getBlue(), COLOR_CYAN.getOpacity() * 0.60));
        gc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.NORMAL, 10));
        gc.fillText(state.name(), cx, cy + AVATAR_RADIUS * 0.55);
    }

    /** Stop the render loop when removed from scene to free resources. */
    public void dispose() {
        renderLoop.stop();
        if (stateTimeline != null) {
            stateTimeline.stop();
        }
    }
}
