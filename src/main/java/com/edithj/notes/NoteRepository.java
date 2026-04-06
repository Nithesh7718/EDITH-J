package com.edithj.notes;

import java.util.List;
import java.util.Optional;

public interface NoteRepository {

    List<Note> findAll();

    Optional<Note> findById(String noteId);

    Note save(Note note);

    boolean deleteById(String noteId);
}
