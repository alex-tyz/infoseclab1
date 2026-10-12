package org.example.secureapi.model;

public record User(long id, String username, String passwordHash) {
}