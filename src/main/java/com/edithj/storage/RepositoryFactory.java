package com.edithj.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.config.AppConfig;
import com.edithj.notes.FileNoteRepository;
import com.edithj.notes.NoteRepository;
import com.edithj.notes.SQLiteNoteRepository;
import com.edithj.memory.InMemoryMemoryRepository;
import com.edithj.memory.MemoryRepository;
import com.edithj.memory.SQLiteMemoryRepository;
import com.edithj.reminders.FileReminderRepository;
import com.edithj.reminders.ReminderRepository;
import com.edithj.reminders.SQLiteReminderRepository;
import com.edithj.chat.ChatRepository;
import com.edithj.chat.SQLiteChatRepository;

public final class RepositoryFactory {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryFactory.class);
    private static final DatabaseManager DATABASE_MANAGER = createDatabaseManager();

    private RepositoryFactory() {
    }

    private static DatabaseManager createDatabaseManager() {
        try {
            AppConfig appConfig = AppConfig.load();
            return new DatabaseManager(appConfig.storageConfig().databasePath());
        } catch (RuntimeException exception) {
            logger.warn("Unable to load storage configuration, using default database path", exception);
            return DatabaseManager.defaultManager();
        }
    }

    public static NoteRepository createNoteRepository() {
        try {
            DATABASE_MANAGER.initialize();
            return new SQLiteNoteRepository(DATABASE_MANAGER);
        } catch (RuntimeException exception) {
            logger.warn("SQLite note repository unavailable, falling back to JSON storage", exception);
            return new FileNoteRepository();
        }
    }

    public static ReminderRepository createReminderRepository() {
        try {
            DATABASE_MANAGER.initialize();
            return new SQLiteReminderRepository(DATABASE_MANAGER);
        } catch (RuntimeException exception) {
            logger.warn("SQLite reminder repository unavailable, falling back to JSON storage", exception);
            return new FileReminderRepository();
        }
    }

    public static MemoryRepository createMemoryRepository() {
        try {
            DATABASE_MANAGER.initialize();
            return new SQLiteMemoryRepository(DATABASE_MANAGER);
        } catch (RuntimeException exception) {
            logger.warn("SQLite memory repository unavailable, falling back to in-memory storage", exception);
            return new InMemoryMemoryRepository();
        }
    }

    public static ChatRepository createChatRepository() {
        try {
            DATABASE_MANAGER.initialize();
            return new SQLiteChatRepository(DATABASE_MANAGER);
        } catch (RuntimeException exception) {
            logger.warn("SQLite chat repository unavailable", exception);
            return new ChatRepository() {
                @Override public java.util.List<com.edithj.chat.ChatMessage> findRecent(int limit) { return java.util.List.of(); }
                @Override public void save(com.edithj.chat.ChatMessage message) {}
                @Override public void clear() {}
            };
        }
    }
}
