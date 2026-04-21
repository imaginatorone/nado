package com.solarl.nado.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.CategoryRepository;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * тесты санитизации FTS-запросов.
 * доказывают что спецсимволы tsquery не пробрасываются в PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class FtsSanitizationTest {

    @Mock private AdRepository adRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserService userService;
    @Mock private WantToBuyService wantToBuyService;
    @Mock private AdStatusTransitionService statusTransitionService;

    @InjectMocks private AdService adService;

    private String sanitize(String raw) throws Exception {
        Method method = AdService.class.getDeclaredMethod("sanitizeFtsQuery", String.class);
        method.setAccessible(true);
        return (String) method.invoke(adService, raw);
    }

    @Test
    @DisplayName("обычный текст без изменений")
    void normalText_preserved() throws Exception {
        assertEquals("велосипед Trek", sanitize("велосипед Trek"));
    }

    @Test
    @DisplayName("спецсимволы tsquery удаляются")
    void specialChars_removed() throws Exception {
        assertEquals("test  query", sanitize("test & query"));
        assertEquals("test  query", sanitize("test | query"));
        assertEquals("test query", sanitize("test !query"));
    }

    @Test
    @DisplayName("кавычки и скобки удаляются")
    void quotes_brackets_removed() throws Exception {
        assertEquals("iphone 15 pro", sanitize("\"iphone\" (15) [pro]"));
    }

    @Test
    @DisplayName("пустой ввод после санитизации")
    void onlySpecialChars_emptyResult() throws Exception {
        assertEquals("", sanitize("!!!???..."));
    }

    @Test
    @DisplayName("кириллица сохраняется")
    void cyrillic_preserved() throws Exception {
        assertEquals("Продам машину", sanitize("Продам машину"));
    }

    @Test
    @DisplayName("несколько пробелов и trim")
    void multipleSpaces_trimmed() throws Exception {
        assertEquals("hello   world", sanitize("  hello   world  "));
    }

    @Test
    @DisplayName("цифры сохраняются")
    void numbers_preserved() throws Exception {
        assertEquals("iPhone 15 Pro 256GB", sanitize("iPhone 15 Pro 256GB"));
    }

    @Test
    @DisplayName("SQL injection attempt sanitized")
    void sqlInjection_sanitized() throws Exception {
        String result = sanitize("test'; DROP TABLE ads;--");
        assertFalse(result.contains("'"));
        assertFalse(result.contains(";"));
        assertFalse(result.contains("--"));
    }
}
