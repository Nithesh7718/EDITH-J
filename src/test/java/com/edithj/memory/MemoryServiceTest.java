package com.edithj.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class MemoryServiceTest {

    @Test
    void memoryService_usesRepositoryContract() {
        InMemoryMemoryRepository repository = new InMemoryMemoryRepository();
        MemoryService service = new MemoryService(repository);

        service.add("prefs", "Dark HUD with cyan accents");
        service.add("tasks", "Finish migration step");

        assertEquals(2, service.recent(10).size());
        assertFalse(service.search("dark").isEmpty());
    }
}
