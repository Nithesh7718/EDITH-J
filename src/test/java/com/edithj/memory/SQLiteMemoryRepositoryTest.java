package com.edithj.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.edithj.storage.DatabaseManager;

class SQLiteMemoryRepositoryTest {

    @Test
    void sqliteMemoryRepository_savesAndQueriesEntries() throws Exception {
        Path dbPath = Files.createTempFile("edithj-memory", ".db");
        dbPath.toFile().deleteOnExit();

        DatabaseManager manager = new DatabaseManager(dbPath);
        manager.initialize();
        SQLiteMemoryRepository repository = new SQLiteMemoryRepository(manager);

        repository.save(new MemoryEntry("prefs", "Use concise responses"));
        repository.save(new MemoryEntry("notes", "Remember SQLite is primary storage"));

        assertEquals(2, repository.findRecent(10).size());
        assertEquals(1, repository.search("sqlite", 10).size());
        assertTrue(repository.search("prefs", 10).get(0).content().contains("concise"));
    }
}
