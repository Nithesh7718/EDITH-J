package com.edithj.resilience;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

public final class RuntimeMetrics {

    private final LongAdder startupSuccess = new LongAdder();
    private final LongAdder startupFailure = new LongAdder();
    private final LongAdder detectionEvents = new LongAdder();
    private final DoubleAdder detectionLatencyMillis = new DoubleAdder();
    private final LongAdder recoveryEvents = new LongAdder();
    private final DoubleAdder recoveryLatencyMillis = new DoubleAdder();
    private final LongAdder providerFailoverAttempts = new LongAdder();
    private final LongAdder providerFailoverSuccesses = new LongAdder();
    private final LongAdder degradedModeEntries = new LongAdder();

    public void recordStartupSuccess() {
        startupSuccess.increment();
    }

    public void recordStartupFailure() {
        startupFailure.increment();
    }

    public void recordDetectionLatency(long millis) {
        detectionEvents.increment();
        detectionLatencyMillis.add(millis);
    }

    public void recordRecoveryLatency(long millis) {
        recoveryEvents.increment();
        recoveryLatencyMillis.add(millis);
    }

    public void recordProviderFailoverAttempt() {
        providerFailoverAttempts.increment();
    }

    public void recordProviderFailoverSuccess() {
        providerFailoverSuccesses.increment();
    }

    public void recordDegradedModeEntry() {
        degradedModeEntries.increment();
    }

    public double startupSuccessRate() {
        long total = startupSuccess.sum() + startupFailure.sum();
        return total == 0 ? 0.0 : (double) startupSuccess.sum() / total;
    }

    public double meanTimeToDetectMillis() {
        long events = detectionEvents.sum();
        return events == 0 ? 0.0 : detectionLatencyMillis.sum() / events;
    }

    public double meanTimeToRecoverMillis() {
        long events = recoveryEvents.sum();
        return events == 0 ? 0.0 : recoveryLatencyMillis.sum() / events;
    }

    public double providerFailoverSuccessRate() {
        long attempts = providerFailoverAttempts.sum();
        return attempts == 0 ? 0.0 : (double) providerFailoverSuccesses.sum() / attempts;
    }

    public long degradedModeContinuityCount() {
        return degradedModeEntries.sum();
    }
}
