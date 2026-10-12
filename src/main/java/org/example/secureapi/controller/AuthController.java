package org.example.secureapi.controller;

import java.util.Map;

import org.example.secureapi.dto.LoginRequest;
import org.example.secureapi.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Нужны username (до 50 символов) и password (до 72 символов)"));
        }
        return authService.login(request.username(), request.password())
                .map(token -> ResponseEntity.ok(Map.of("token", token, "tokenType", "Bearer")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Неверный логин или пароль")));
    }
}