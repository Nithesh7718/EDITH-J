package com.edithj.app;

import com.edithj.api.EdithApiServer;
import com.edithj.config.AppConfig;
import com.edithj.storage.DatabaseManager;
import com.edithj.storage.JsonToSqliteMigrationService;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;

public final class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    private Launcher() {
    }

    public static void main(String[] args) {
        logger.info("Starting EDITH-J Backend Service...");

        AppConfig appConfig = AppConfig.load();
        DatabaseManager databaseManager = new DatabaseManager(appConfig.storageConfig().databasePath());
        new JsonToSqliteMigrationService(databaseManager).migrateOnce();

        Javalin app = new EdithApiServer().createApp();
        try {
            app.start(8080);
            logger.info("EDITH-J is ready. Visit http://localhost:8080 to open the UI.");
        } catch (Exception e) {
            logger.warn("Server failed to start on port 8080. It might already be running: {}", e.getMessage());
        }

        openBrowser("http://localhost:8080");
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback for some Windows environments if Desktop API fails
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception e) {
            logger.error("Failed to open browser", e);
        }
    }
}

