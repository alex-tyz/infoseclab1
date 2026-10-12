package org.example.secureapi.dto;

public record LoginRequest(String username, String password) {
    public boolean isValid() {
        return username != null && !username.isBlank() && username.length() <= 50
                && password != null && !password.isEmpty() && password.length() <= 72;
    }
}
