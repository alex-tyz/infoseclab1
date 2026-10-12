package org.example.secureapi.dto;

public record NoteRequest(String title, String content) {

    /** Лимиты совпадают с VARCHAR в schema.sql. */
    public boolean isValid() {
        return title != null && !title.isBlank() && title.length() <= 100
                && content != null && !content.isBlank() && content.length() <= 1000;
    }
}
