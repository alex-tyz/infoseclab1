package org.example.secureapi.repository;


import org.example.secureapi.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash")
    );

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByUsername(String username) {
        List<User> result = jdbc.query(
                "SELECT id, username, password_hash FROM users WHERE username = ?",
                USER_ROW_MAPPER,
                username);
        return result.stream().findFirst();
    }
    public void save(String username, String passwordHash) {
        jdbc.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, passwordHash);
    }
    public List<User> findAll() {
        return jdbc.query("SELECT id, username, password_hash FROM users ORDER BY id", USER_ROW_MAPPER);
    }
}
