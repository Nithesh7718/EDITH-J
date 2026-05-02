package com.edithj.app;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.api.EdithApiServer;
import com.edithj.config.AppConfig;
import com.edithj.config.AppPaths;
import com.edithj.resilience.FailureType;
import com.edithj.resilience.HealthMonitorRegistry;
import com.edithj.resilience.HealthSignal;
import com.edithj.resilience.IncidentSeverity;
import com.edithj.storage.DatabaseManager;
import com.edithj.storage.JsonToSqliteMigrationService;

import io.javalin.Javalin;

public final class Launcher {

    static {
        AppPaths.bootstrapSystemProperties();
    }

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    private Launcher() {
    }

    public static void main(String[] args) {
        logger.info("Starting EDITH-J Backend Service...");
        HealthMonitorRegistry healthRegistry = HealthMonitorRegistry.instance();
        DatabaseManager databaseManager;

        try {
            AppPaths.ensureRuntimeDirectories();
            AppConfig appConfig = AppConfig.load();
            databaseManager = new DatabaseManager(appConfig.storageConfig().databasePath());
            healthRegistry.initialize(databaseManager);

            if (!EdithApiServer.hasBundledFrontend()) {
                String message = "EDITH-J could not start because the bundled frontend assets are missing.";
                healthRegistry.registerHealthSignal(new HealthSignal(
                        "frontend",
                        FailureType.FRONTEND,
                        IncidentSeverity.HIGH,
                        "Packaged frontend assets are missing from the runtime.",
                        java.time.Instant.now(),
                        Map.of("entry", "/public/index.html")));
                logger.error("{} Rebuild the app before packaging.", message);
                showStartupDialog("EDITH-J Startup Error", message + System.lineSeparator()
                        + "Rebuild with 'mvn clean package' before creating the installer.");
                healthRegistry.markStartupFailure();
                return;
            }

            new JsonToSqliteMigrationService(databaseManager).migrateOnce();

            int port = appConfig.appPort();
            String host = appConfig.appHost();
            String url = "http://localhost:" + port;

            Javalin app = new EdithApiServer().createApp();
            app.start(host, port);
            logger.info("EDITH-J is ready. Visit {} to open the UI.", url);
            healthRegistry.markStartupSuccess();

            if (appConfig.isAutoOpenBrowserEnabled()) {
                openBrowser(url);
            }
        } catch (Exception e) {
            String message = userFacingStartupMessage(e);
            logger.error("EDITH-J failed to start", e);
            healthRegistry.registerHealthSignal(new HealthSignal(
                    "startup",
                    FailureType.STARTUP,
                    IncidentSeverity.CRITICAL,
                    message,
                    java.time.Instant.now(),
                    Map.of("errorClass", e.getClass().getSimpleName())));
            healthRegistry.markStartupFailure();
            showStartupDialog("EDITH-J Startup Error", message);
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback for some Windows environments if Desktop API fails
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            }
        } catch (IOException | URISyntaxException | SecurityException e) {
            logger.error("Failed to open browser", e);
        }
    }

    private static String userFacingStartupMessage(Exception exception) {
        if (exception == null) {
            return "EDITH-J could not start cleanly."
                    + System.lineSeparator()
                    + "Details: Unknown startup error.";
        }

        String rawMessage = exception.getMessage() == null ? "" : exception.getMessage();
        String lower = rawMessage.toLowerCase();

        if (lower.contains("address already in use") || lower.contains("failed to bind")) {
            return "EDITH-J could not start because its local web port is already in use."
                    + System.lineSeparator()
                    + "Close the other process using that port or change app.port in your configuration.";
        }
        if (lower.contains("unable to create storage directory")
                || lower.contains("unable to initialize storage")
                || lower.contains("unable to initialize sqlite schema")) {
            return "EDITH-J could not prepare its local data storage."
                    + System.lineSeparator()
                    + "Check that your user profile folders are writable and try again.";
        }
        return "EDITH-J could not start cleanly."
                + System.lineSeparator()
                + "Details: " + (rawMessage.isBlank() ? exception.getClass().getSimpleName() : rawMessage);
    }

    private static void showStartupDialog(String title, String message) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            javax.swing.JOptionPane.showMessageDialog(null, message, title, javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ignored) {
            // Keep startup failure reporting best-effort only.
        }
    }
}
