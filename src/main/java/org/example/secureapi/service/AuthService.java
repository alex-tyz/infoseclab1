package org.example.secureapi.service;

import org.example.secureapi.model.User;
import org.example.secureapi.repository.UserRepository;
import org.example.secureapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    //хеш случайной строки для ненайденого пользователся для сверки
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }
        /** Возвращает JWT, если логин и пароль верные, иначе пусто. */
        public Optional<String> login(String username, String password) {
            Optional<User> user = users.findByUsername(username);

            // bcrypt выполняется ВСЕГДА — даже если пользователя нет
            String hash = user.map(User::passwordHash).orElse(dummyHash);
            boolean passwordOk = passwordEncoder.matches(password, hash);

            if (user.isPresent() && passwordOk) {
                return Optional.of(jwtService.generateToken(username));
            }
            return Optional.empty();
        }


}
