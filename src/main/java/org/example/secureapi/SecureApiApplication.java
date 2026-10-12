package org.example.secureapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа. Spring Boot сам поднимет веб-сервер на порту 8080
 * и найдёт все классы с аннотациями (@RestController и т.д.) в этом пакете и ниже.
 */
@SpringBootApplication
public class SecureApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureApiApplication.class, args);
    }
}
