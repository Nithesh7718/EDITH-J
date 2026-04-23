package com.edithj.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

class SettingsViewFxmlTest {

    private static final AtomicBoolean FX_INITIALIZED = new AtomicBoolean(false);

    @BeforeAll
    static void initializeJavaFx() throws InterruptedException {
        if (FX_INITIALIZED.compareAndSet(false, true)) {
            CountDownLatch startupLatch = new CountDownLatch(1);
            Platform.startup(startupLatch::countDown);
            if (!startupLatch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX toolkit did not initialize in time");
            }
        }
    }

    @Test
    void settingsViewFxml_loadsWithoutControllerErrors() throws Exception {
        URL resource = SettingsViewFxmlTest.class.getResource("/fxml/settings-view.fxml");
        assertNotNull(resource, "Missing settings-view.fxml resource");

        CountDownLatch loadLatch = new CountDownLatch(1);
        final Parent[] rootHolder = new Parent[1];
        final Exception[] errorHolder = new Exception[1];

        Platform.runLater(() -> {
            try {
                rootHolder[0] = new FXMLLoader(resource).load();
            } catch (Exception exception) {
                errorHolder[0] = exception;
            } finally {
                loadLatch.countDown();
            }
        });

        if (!loadLatch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out loading settings-view.fxml");
        }
        if (errorHolder[0] != null) {
            throw new AssertionError("settings-view.fxml failed to load", errorHolder[0]);
        }
        assertNotNull(rootHolder[0], "settings-view.fxml produced null root node");
    }
}
