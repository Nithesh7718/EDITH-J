package com.edithj.app;

import com.edithj.config.AppConfig;
import com.edithj.storage.DatabaseManager;
import com.edithj.storage.JsonToSqliteMigrationService;
import com.edithj.ui.navigation.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application entry point for EDITH-J.
 * 
 * <p><b>Memory Tuning Note:</b>
 * If running into native memory limits due to Vosk and JavaFX Marlin rendering,
 * the app can be run with JVM options like:
 * {@code -Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m -Xss256k}
 * to constrain the Java heap and leave more off-heap memory room for native allocations.
 */
public class EdithApplication extends Application {

    @Override
    public void start(Stage stage) {
        AppConfig appConfig = AppConfig.load();
        DatabaseManager databaseManager = new DatabaseManager(appConfig.storageConfig().databasePath());
        new JsonToSqliteMigrationService(databaseManager).migrateOnce();

        SceneManager sceneManager = new SceneManager();
        sceneManager.showMainWindow(stage);
    }
}
