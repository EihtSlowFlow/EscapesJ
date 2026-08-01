package io.github.ramiro.escapesj.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void testFormatoLocal_FechasValidas() {
        assertEquals("15/08/2026", DateUtil.formatoLocal("2026-08-15"));
        assertEquals("01/01/2026", DateUtil.formatoLocal("2026-01-01"));
        assertEquals("05/12/2023", DateUtil.formatoLocal("2023-12-05"));
    }

    @Test
    void testFormatoLocal_FechaInvalidaOFormateada() {
        // Fallback al valor original
        assertEquals("15/08/2026", DateUtil.formatoLocal("15/08/2026"));
        assertEquals("fecha_falsa", DateUtil.formatoLocal("fecha_falsa"));
        assertEquals("2026-15-01", DateUtil.formatoLocal("2026-15-01")); // Mes 15 es inválido, fallback
    }

    @Test
    void testFormatoLocal_NullOVacio() {
        assertEquals("", DateUtil.formatoLocal(null));
        assertEquals("", DateUtil.formatoLocal(""));
        assertEquals("", DateUtil.formatoLocal("   "));
    }
}
