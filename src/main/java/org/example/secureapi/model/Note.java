package org.example.secureapi.model;

import java.time.LocalDateTime;

public record Note(long id, String owner, String title, String content, LocalDateTime createdAt) {
}