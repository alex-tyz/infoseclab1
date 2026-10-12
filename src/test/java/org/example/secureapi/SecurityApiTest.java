package org.example.secureapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import com.jayway.jsonpath.JsonPath;
import org.example.secureapi.model.User;
import org.example.secureapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Интеграционные тесты мер защиты: поднимается всё приложение, запросы идут через фильтры.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityApiTest {

    // Секреты генерируются случайно при каждом запуске — в репозитории их нет
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ADMIN_PASSWORD = randomBase64(18);
    private static final String USER_PASSWORD = randomBase64(18);

    @DynamicPropertySource
    static void secrets(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> randomBase64(32));
        registry.add("DEMO_ADMIN_PASSWORD", () -> ADMIN_PASSWORD);
        registry.add("DEMO_USER_PASSWORD", () -> USER_PASSWORD);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    // ---------- Аутентификация ----------

    @Test
    void loginWithCorrectPasswordReturnsJwt() throws Exception {
        String token = login("admin", ADMIN_PASSWORD);
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    void wrongPasswordAndUnknownUserGiveSameResponse() throws Exception {
        String wrongPassword = postJson("/auth/login", "{\"username\":\"admin\",\"password\":\"wrong\"}", null)
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String unknownUser = postJson("/auth/login", "{\"username\":\"nobody\",\"password\":\"wrong\"}", null)
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    void loginWithoutPasswordIsBadRequest() throws Exception {
        postJson("/auth/login", "{\"username\":\"admin\"}", null).andExpect(status().isBadRequest());
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        User admin = users.findByUsername("admin").orElseThrow();
        assertThat(admin.passwordHash()).startsWith("$2a$12$").doesNotContain(ADMIN_PASSWORD);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.passwordHash())).isTrue();
    }

    // ---------- Доступ к защищённым эндпоинтам ----------

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/data"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void forgedTokenIs401() throws Exception {
        mvc.perform(get("/api/data").header(HttpHeaders.AUTHORIZATION, "Bearer fake.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithTamperedPayloadIs401() throws Exception {
        String[] parts = login("user", USER_PASSWORD).split("\\.");
        String fakePayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + fakePayload + "." + parts[2];

        mvc.perform(get("/api/data").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dataWithValidTokenDoesNotLeakPasswordHashes() throws Exception {
        String body = mvc.perform(get("/api/data").header(HttpHeaders.AUTHORIZATION, "Bearer " + login("admin", ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<String>read(body, "$.currentUser")).isEqualTo("admin");
        assertThat(body).doesNotContain("password").doesNotContain("$2a$");
    }

    // ---------- Заметки: владелец, XSS, SQLi ----------

    @Test
    void usersSeeOnlyTheirOwnNotesAndCannotSpoofOwner() throws Exception {
        String userToken = login("user", USER_PASSWORD);
        postJson("/api/notes", "{\"title\":\"owner-test\",\"content\":\"secret of user\",\"owner\":\"admin\"}", userToken)
                .andExpect(status().isCreated());

        assertThat(getNotes(userToken)).contains("secret of user");
        assertThat(getNotes(login("admin", ADMIN_PASSWORD))).doesNotContain("secret of user");
    }

    @Test
    void htmlInNotesIsEscapedInResponse() throws Exception {
        String token = login("admin", ADMIN_PASSWORD);
        postJson("/api/notes", "{\"title\":\"xss\",\"content\":\"<script>alert(1)</script>\"}", token)
                .andExpect(status().isCreated());

        String body = getNotes(token);
        assertThat(body).doesNotContain("<script>").contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void sqlInjectionInLoginDoesNotBypassAuth() throws Exception {
        postJson("/auth/login", "{\"username\":\"admin' OR '1'='1\",\"password\":\"x' OR '1'='1\"}", null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sqlInjectionInNoteIsStoredAsPlainText() throws Exception {
        String token = login("admin", ADMIN_PASSWORD);
        postJson("/api/notes", "{\"title\":\"sqli\",\"content\":\"'); DROP TABLE notes; --\"}", token)
                .andExpect(status().isCreated());

        // Таблица на месте, а "команда" сохранилась как обычный текст (экранированный для вывода)
        assertThat(getNotes(token)).contains("DROP TABLE notes");
    }

    @Test
    void tooLongNoteIsRejected() throws Exception {
        String content = "a".repeat(1001);
        postJson("/api/notes", "{\"title\":\"t\",\"content\":\"" + content + "\"}", login("admin", ADMIN_PASSWORD))
                .andExpect(status().isBadRequest());
    }

    // ---------- Ошибки и заголовки ----------

    @Test
    void malformedJsonGivesShortErrorWithoutDetails() throws Exception {
        String body = postJson("/auth/login", "{bad json", null)
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).doesNotContain("Exception").doesNotContain("at org.");
    }

    @Test
    void securityHeadersArePresentEvenOn401() throws Exception {
        mvc.perform(get("/api/data"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    // ---------- Помощники ----------

    private String login(String username, String password) throws Exception {
        String body = postJson("/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", null)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    private String getNotes(String token) throws Exception {
        return mvc.perform(get("/api/notes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions postJson(String url, String json, String token) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mvc.perform(request);
    }

    private static String randomBase64(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return Base64.getEncoder().encodeToString(buf);
    }
}
