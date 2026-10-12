package org.example.secureapi.config;

import org.example.secureapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * При старте создаёт демо-пользователей admin и user.
 * Пароли берутся из окружения / .env, в БД сохраняется только bcrypt-хэш.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String userPassword;

    public DataInitializer(UserRepository users,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.demo.admin-password}") String adminPassword,
                           @Value("${app.demo.user-password}") String userPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.userPassword = userPassword;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", adminPassword);
        createIfMissing("user", userPassword);
    }

    private void createIfMissing(String username, String rawPassword) {
        if (users.findByUsername(username).isEmpty()) {
            users.save(username, passwordEncoder.encode(rawPassword));
            // В лог — только имя. Ни пароль, ни даже хэш в логи не пишем.
            log.info("Создан демо-пользователь: {}", username);
        }
    }
}
