package org.example.secureapi.repository;

import java.util.List;

import org.example.secureapi.model.Note;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository {

    private static final RowMapper<Note> NOTE_ROW_MAPPER = (rs, rowNum) -> new Note(
            rs.getLong("id"),
            rs.getString("owner"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public NoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Только заметки указанного владельца — чужие не вернутся никогда. */
    public List<Note> findByOwner(String owner) {
        return jdbc.query(
                "SELECT id, owner, title, content, created_at FROM notes WHERE owner = ? ORDER BY id",
                NOTE_ROW_MAPPER,
                owner);
    }

    public void save(String owner, String title, String content) {
        jdbc.update("INSERT INTO notes (owner, title, content) VALUES (?, ?, ?)",
                owner, title, content);
    }
}