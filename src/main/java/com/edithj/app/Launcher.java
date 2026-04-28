package com.edithj.app;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.api.ApiServer;
import com.edithj.assistant.AssistantService;
import com.edithj.assistant.AssistantStatusProbe;
import com.edithj.assistant.AssistantStatusService;
import com.edithj.config.AppConfig;
import com.edithj.notes.NoteService;
import com.edithj.reminders.ReminderService;
import com.edithj.storage.DatabaseManager;
import com.edithj.storage.JsonToSqliteMigrationService;
import com.edithj.storage.RepositoryFactory;

public final class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);
    private static final int DEFAULT_PORT = 7070;

    private Launcher() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();

        // Initialise storage
        DatabaseManager databaseManager = new DatabaseManager(config.storageConfig().databasePath());
        new JsonToSqliteMigrationService(databaseManager).migrateOnce();

        // Build services
        NoteService noteService = new NoteService(RepositoryFactory.createNoteRepository());
        ReminderService reminderService = new ReminderService(RepositoryFactory.createReminderRepository());
        AssistantService assistantService = new AssistantService();
        AssistantStatusService statusService = AssistantStatusService.instance();

        // Probe Groq connectivity in background so startup is not blocked
        CompletableFuture.runAsync(new AssistantStatusProbe()::runStartupProbe);

        int port = resolvePort(config);
        logger.info("Starting EDITH-J API server on port {}", port);
        ApiServer.create(assistantService, noteService, reminderService, statusService).start(port);
    }

    private static int resolvePort(AppConfig config) {
        String envPort = config.envConfig().getOrDefault("SERVER_PORT",
                config.properties().getProperty("server.port", String.valueOf(DEFAULT_PORT)));
        try {
            return Integer.parseInt(envPort.trim());
        } catch (NumberFormatException ex) {
            logger.warn("Invalid SERVER_PORT value '{}', defaulting to {}", envPort, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}

