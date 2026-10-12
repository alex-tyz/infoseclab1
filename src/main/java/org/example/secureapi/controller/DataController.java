package org.example.secureapi.controller;

import java.util.List;
import java.util.Map;

import org.example.secureapi.dto.UserView;
import org.example.secureapi.repository.UserRepository;
import org.example.secureapi.security.JwtAuthFilter;
import org.example.secureapi.security.OutputEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DataController {

    private final UserRepository users;

    public DataController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/data")
    public Map<String, Object> getData(
            @RequestAttribute(JwtAuthFilter.USERNAME_ATTRIBUTE) String currentUser) {
        // Хэш пароля наружу не отдаём (UserView), строковые данные экранируем (XSS)
        List<UserView> list = users.findAll().stream()
                .map(u -> new UserView(u.id(), OutputEncoder.html(u.username())))
                .toList();
        return Map.of("currentUser", OutputEncoder.html(currentUser), "users", list);
    }
}