package com.edithj.memory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.edithj.storage.DatabaseManager;

public class SQLiteMemoryRepository implements MemoryRepository {

    private final DatabaseManager databaseManager;

    public SQLiteMemoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public MemoryEntry save(MemoryEntry entry) {
        String sql = """
                INSERT INTO memory_entries (id, category, content, created_at)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.id());
            statement.setString(2, entry.category());
            statement.setString(3, entry.content());
            statement.setString(4, entry.createdAt().toString());
            statement.executeUpdate();
            return entry;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save memory entry", exception);
        }
    }

    @Override
    public List<MemoryEntry> findRecent(int limit) {
        int safeLimit = Math.max(1, limit);
        String sql = """
                SELECT id, category, content, created_at
                FROM memory_entries
                ORDER BY created_at DESC
                LIMIT ?
                """;

        List<MemoryEntry> entries = new ArrayList<>();
        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapMemory(resultSet));
                }
            }
            return entries;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read memory entries", exception);
        }
    }

    @Override
    public List<MemoryEntry> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return findRecent(limit);
        }

        int safeLimit = Math.max(1, limit);
        String normalized = "%" + query.trim().toLowerCase() + "%";

        String sql = """
                SELECT id, category, content, created_at
                FROM memory_entries
                WHERE LOWER(category) LIKE ? OR LOWER(content) LIKE ?
                ORDER BY created_at DESC
                LIMIT ?
                """;

        List<MemoryEntry> entries = new ArrayList<>();
        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, normalized);
            statement.setInt(3, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapMemory(resultSet));
                }
            }
            return entries;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to search memory entries", exception);
        }
    }

    private MemoryEntry mapMemory(ResultSet resultSet) throws SQLException {
        return new MemoryEntry(
                resultSet.getString("id"),
                resultSet.getString("category"),
                resultSet.getString("content"),
                Instant.parse(resultSet.getString("created_at"))
        );
    }
}
