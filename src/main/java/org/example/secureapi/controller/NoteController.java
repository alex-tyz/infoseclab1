package org.example.secureapi.controller;

import java.util.List;
import java.util.Map;

import org.example.secureapi.dto.NoteRequest;
import org.example.secureapi.dto.NoteView;
import org.example.secureapi.repository.NoteRepository;
import org.example.secureapi.security.JwtAuthFilter;
import org.example.secureapi.security.OutputEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteRepository notes;

    public NoteController(NoteRepository notes) {
        this.notes = notes;
    }

    /** Владелец заметки — из проверенного токена, а не из тела запроса. */
    @PostMapping
    public ResponseEntity<Map<String, String>> create(
            @RequestAttribute(JwtAuthFilter.USERNAME_ATTRIBUTE) String currentUser,
            @RequestBody NoteRequest request) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Нужны title (до 100 символов) и content (до 1000 символов)"));
        }
        notes.save(currentUser, request.title(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "created"));
    }

    /** Только заметки текущего пользователя. Пользовательский текст экранируется (защита от XSS). */
    @GetMapping
    public List<NoteView> list(@RequestAttribute(JwtAuthFilter.USERNAME_ATTRIBUTE) String currentUser) {
        return notes.findByOwner(currentUser).stream()
                .map(n -> new NoteView(
                        n.id(),
                        OutputEncoder.html(n.title()),
                        OutputEncoder.html(n.content()),
                        n.createdAt().toString()))
                .toList();
    }
}
