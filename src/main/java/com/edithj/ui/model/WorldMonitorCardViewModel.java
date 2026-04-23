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
    private final StringProperty worldMonitorMeta = new SimpleStringProperty("Last update: never");
    private final StringProperty localKbMeta = new SimpleStringProperty("Last sync: pending");

    public StringProperty worldMonitorStatusProperty() {
        return worldMonitorStatus;
    }

    public StringProperty localKbStatusProperty() {
        return localKbStatus;
    }

    public StringProperty worldMonitorMetaProperty() {
        return worldMonitorMeta;
    }

    public StringProperty localKbMetaProperty() {
        return localKbMeta;
    }

    public void onWorldDataUpdated(WorldSnapshot snapshot) {
        if (snapshot == null || snapshot.fetchedAt() == null) {
            worldMonitorStatus.set("Unconfigured");
            worldMonitorMeta.set("Last update: pending");
            return;
        }

        Duration age = Duration.between(snapshot.fetchedAt(), Instant.now());
        long minutes = Math.max(0L, age.toMinutes());
        worldMonitorStatus.set("Configured");
        worldMonitorMeta.set("Last update " + minutes + " min ago");
    }

    public void setWorldStatus(String status, String meta) {
        if (status == null || status.isBlank()) {
            worldMonitorStatus.set("Unconfigured");
        } else {
            worldMonitorStatus.set(status.trim());
        }
        if (meta == null || meta.isBlank()) {
            worldMonitorMeta.set("Last update: pending");
        } else {
            worldMonitorMeta.set(meta.trim());
        }
    }

    public void setLocalKbStatus(String status) {
        if (status == null || status.isBlank()) {
            localKbStatus.set("Placeholder mode");
            return;
        }
        localKbStatus.set(status.trim());
    }

    public void setLocalKbMeta(String meta) {
        if (meta == null || meta.isBlank()) {
            localKbMeta.set("Last sync: pending");
            return;
        }
        localKbMeta.set(meta.trim());
    }
}
