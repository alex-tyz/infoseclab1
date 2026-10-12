package org.example.secureapi.security;

import org.springframework.web.util.HtmlUtils;

/**
 * Защита от XSS: экранирование пользовательских данных перед отправкой в ответе.
 *
 * Символы < > " ' & превращаются в HTML-сущности (&lt; &gt; &quot; &#39; &amp;).
 * Если фронтенд вставит такую строку в страницу (например, через innerHTML),
 * браузер покажет её как текст, а не выполнит как код.
 *
 * Экранируем на ВЫХОДЕ, а в БД храним исходный текст: так данные не портятся
 * и их можно корректно выдать в другом формате, если понадобится.
 */
public final class OutputEncoder {

    private OutputEncoder() {
    }

    public static String html(String value) {
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }
}
