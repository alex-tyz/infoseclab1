package org.example.secureapi.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Второй слой защиты от XSS и других атак через браузер: заголовки безопасности.
 * Стоит ПЕРВЫМ в цепочке фильтров, поэтому заголовки попадают в любой ответ, включая 401.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Браузер не будет "угадывать" тип: JSON никогда не исполнится как HTML/JS
        response.setHeader("X-Content-Type-Options", "nosniff");
        // API не отдаёт HTML, поэтому запрещаем вообще любые скрипты, стили, фреймы
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        // Защита от clickjacking для старых браузеров, не понимающих CSP frame-ancestors
        response.setHeader("X-Frame-Options", "DENY");
        // Ответы с личными данными и токенами не должны оседать в кэшах
        response.setHeader("Cache-Control", "no-store");
        // Не передавать адрес нашего API сторонним сайтам
        response.setHeader("Referrer-Policy", "no-referrer");
        chain.doFilter(request, response);
    }
}
