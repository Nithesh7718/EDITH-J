package com.edithj.app;

import com.edithj.config.AppConfig;
import com.edithj.storage.DatabaseManager;
import com.edithj.storage.JsonToSqliteMigrationService;
import com.edithj.ui.navigation.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

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
