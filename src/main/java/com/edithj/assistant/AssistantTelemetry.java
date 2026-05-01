package com.edithj.assistant;

import java.util.concurrent.atomic.LongAdder;

/**
 * Lightweight in-process telemetry counters for EDITH routing behavior.
 */
public final class AssistantTelemetry {

    private static final AssistantTelemetry INSTANCE = new AssistantTelemetry();

    private final LongAdder clarificationPrompts = new LongAdder();
    private final LongAdder worldCircuitOpenHits = new LongAdder();
    private final LongAdder localKbEmptyHits = new LongAdder();

    private AssistantTelemetry() {
    }

    public static AssistantTelemetry instance() {
        return INSTANCE;
    }

    public void recordClarificationPrompt() {
        clarificationPrompts.increment();
    }

    public void recordWorldCircuitOpenHit() {
        worldCircuitOpenHits.increment();
    }

    public void recordLocalKbEmptyHit() {
        localKbEmptyHits.increment();
    }

    public Snapshot snapshot() {
        return new Snapshot(
                clarificationPrompts.sum(),
                worldCircuitOpenHits.sum(),
                localKbEmptyHits.sum());
    }

    public void reset() {
        clarificationPrompts.reset();
        worldCircuitOpenHits.reset();
        localKbEmptyHits.reset();
    }

    public record Snapshot(long clarificationPrompts, long worldCircuitOpenHits, long localKbEmptyHits) {
    }
}
