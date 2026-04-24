package com.edithj.app;

import io.javalin.Javalin;
import com.edithj.config.AppConfig;
import com.edithj.storage.DatabaseManager;
import com.edithj.storage.JsonToSqliteMigrationService;
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

        Javalin app = Javalin.create(config -> {
            // Serve React frontend files
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            
            // Allow CORS for local React dev server
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(8080);

        app.get("/api/health", ctx -> ctx.result("EDITH-J Backend is running"));

        logger.info("EDITH-J Backend is ready on port 8080.");
    }
}
