package com.edithj.app;

import com.edithj.api.EdithApiServer;
import com.edithj.config.AppConfig;
import com.edithj.storage.DatabaseManager;
import com.edithj.storage.JsonToSqliteMigrationService;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        app.start(8080);

        logger.info("EDITH-J is ready. Visit http://localhost:8080 to open the UI.");
    }
}

