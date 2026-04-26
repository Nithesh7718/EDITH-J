package com.edithj.integration.worldmonitor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Polling scaffolding for incremental world data updates (scheduler can be
 * plugged later).
 */
public class WorldMonitorPollingService {

    private final WorldMonitorClient worldMonitorClient;
    private Consumer<WorldSnapshot> onWorldDataUpdated = snapshot -> {
    };
    private Duration interval = Duration.ofMinutes(5);
    private Instant lastUpdate;

    public WorldMonitorPollingService(WorldMonitorClient worldMonitorClient) {
        this.worldMonitorClient = Objects.requireNonNull(worldMonitorClient, "worldMonitorClient");
    }

    public void configureInterval(Duration interval) {
        if (interval != null && !interval.isNegative() && !interval.isZero()) {
            this.interval = interval;
        }
    }

    public Duration interval() {
        return interval;
    }

    public Instant lastUpdate() {
        return lastUpdate;
    }

    public void setOnWorldDataUpdated(Consumer<WorldSnapshot> callback) {
        this.onWorldDataUpdated = callback == null ? snapshot -> {
        } : callback;
    }

    public WorldSnapshot pollNow(String region, String isoCode, List<String> symbols) {
        WorldSnapshot snapshot = worldMonitorClient.getWorldSnapshot(region, isoCode, symbols);
        lastUpdate = snapshot.fetchedAt();
        onWorldDataUpdated.accept(snapshot);
        return snapshot;
    }
}
