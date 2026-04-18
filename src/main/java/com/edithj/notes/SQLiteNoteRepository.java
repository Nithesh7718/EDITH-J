package com.edithj.notes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.edithj.storage.DatabaseManager;

public class SQLiteNoteRepository implements NoteRepository {

    private final DatabaseManager databaseManager;

    public SQLiteNoteRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<Note> findAll() {
        String sql = "SELECT id, title, content, created_at, updated_at FROM notes";
        List<Note> notes = new ArrayList<>();

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                notes.add(mapNote(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to list notes", exception);
        }

        return notes;
    }

    @Override
    public Optional<Note> findById(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return Optional.empty();
        }

        String sql = "SELECT id, title, content, created_at, updated_at FROM notes WHERE id = ?";
        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, noteId.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapNote(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load note", exception);
        }

        return Optional.empty();
    }

    @Override
    public Note save(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }

        String sql = """
                INSERT INTO notes (id, title, content, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title,
                    content = excluded.content,
                    updated_at = excluded.updated_at
                """;

        Instant createdAt = note.getCreatedAt() == null ? Instant.now() : note.getCreatedAt();
        Instant updatedAt = note.getUpdatedAt() == null ? Instant.now() : note.getUpdatedAt();
        note.setCreatedAt(createdAt);
        note.setUpdatedAt(updatedAt);

        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, note.getId());
            statement.setString(2, note.getTitle());
            statement.setString(3, note.getContent());
            statement.setString(4, createdAt.toString());
            statement.setString(5, updatedAt.toString());
            statement.executeUpdate();
            return note;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save note", exception);
        }
    }

    @Override
    public boolean deleteById(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return false;
        }

        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection connection = databaseManager.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, noteId.trim());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete note", exception);
        }
    }

    private Note mapNote(ResultSet resultSet) throws SQLException {
        return new Note(
                resultSet.getString("id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }
}
