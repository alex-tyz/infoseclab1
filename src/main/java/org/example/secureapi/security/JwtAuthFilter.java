package org.example.secureapi.security;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_PATHS = Set.of("/auth/login", "/ping");
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String USERNAME_ATTRIBUTE = "authenticatedUsername";
    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            reject(response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        Optional<String> username = jwtService.validateAndGetUsername(token);
        if (username.isEmpty()) {
            reject(response);
            return;
        }

        request.setAttribute(USERNAME_ATTRIBUTE, username.get());
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Требуется действительный токен\"}");
    }
}
