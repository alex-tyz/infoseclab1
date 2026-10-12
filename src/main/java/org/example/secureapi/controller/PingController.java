package org.example.secureapi.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Временный эндпоинт, чтобы проверить, что сервер вообще запускается.
 * GET /ping -> {"status":"ok"}
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
