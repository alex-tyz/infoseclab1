package org.example.secureapi.dto;

/** То, что отдаём клиенту. Поля owner нет — пользователь видит только свои заметки. */
public record NoteView(long id, String title, String content, String createdAt) {
}
