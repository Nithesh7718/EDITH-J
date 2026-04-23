package com.edithj.ui.model;

import java.time.Duration;
import java.time.Instant;

import com.edithj.integration.worldmonitor.WorldSnapshot;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * View-model scaffold for world/local knowledge card status text updates.
 */
public class WorldMonitorCardViewModel {

    private final StringProperty worldMonitorStatus = new SimpleStringProperty("Last update: pending");
    private final StringProperty localKbStatus = new SimpleStringProperty("0 docs indexed • last sync pending");

    public StringProperty worldMonitorStatusProperty() {
        return worldMonitorStatus;
    }

    public StringProperty localKbStatusProperty() {
        return localKbStatus;
    }

    public void onWorldDataUpdated(WorldSnapshot snapshot) {
        if (snapshot == null || snapshot.fetchedAt() == null) {
            worldMonitorStatus.set("Last update: pending");
            return;
        }

        Duration age = Duration.between(snapshot.fetchedAt(), Instant.now());
        long minutes = Math.max(0L, age.toMinutes());
        worldMonitorStatus.set("Last update " + minutes + " min ago");
    }

    public void setLocalKbStatus(String status) {
        if (status == null || status.isBlank()) {
            localKbStatus.set("0 docs indexed • last sync pending");
            return;
        }
        localKbStatus.set(status.trim());
    }
}
