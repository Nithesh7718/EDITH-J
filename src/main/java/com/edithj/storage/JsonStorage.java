package com.edithj.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Thread-safe JSON storage handler with automatic ObjectMapper configuration
 * for Java time types and pretty-printing.
 */
public class JsonStorage {

    private final Path storagePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public JsonStorage(Path storagePath) {
        this.storagePath = storagePath;
        this.objectMapper = createObjectMapper();
        ensureStorageExists();
    }

    /**
     * Read list of objects from JSON file.
     */
    public <T> List<T> readList(TypeReference<List<T>> typeReference) {
        lock.readLock().lock();
        try {
            if (!Files.exists(storagePath)) {
                return new ArrayList<>();
            }

            try (InputStream inputStream = Files.newInputStream(storagePath)) {
                List<T> items = objectMapper.readValue(inputStream, typeReference);
                return items == null ? new ArrayList<>() : items;
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to read storage at " + storagePath, exception);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Write list of objects to JSON file.
     */
    public <T> void writeList(List<T> items) {
        lock.writeLock().lock();
        try {
            ensureStorageExists();
            try (OutputStream outputStream = Files.newOutputStream(storagePath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, items);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to write storage at " + storagePath, exception);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Create a new ObjectMapper with standard configuration for persistence.
     */
    private ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Ensure storage file and parent directories exist.
     */
    private void ensureStorageExists() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(storagePath)) {
                Files.writeString(storagePath, "[]");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize storage at " + storagePath, exception);
        }
    }
}
