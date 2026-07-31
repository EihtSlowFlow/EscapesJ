package io.github.ramiro.escapesj.sdk;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtil {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter LOCAL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateUtil() {} // utility

    /**
     * Convierte una fecha en formato ISO (yyyy-MM-dd) a formato local (dd/MM/yyyy).
     * Si la entrada ya está en formato local o es inválida, se devuelve intacta.
     */
    public static String formatoLocal(String fechaIso) {
        if (fechaIso == null || fechaIso.isBlank()) {
            return "";
        }
        try {
            LocalDate date = LocalDate.parse(fechaIso, ISO_FORMATTER);
            return date.format(LOCAL_FORMATTER);
        } catch (DateTimeParseException e) {
            return fechaIso;
        }
    }
}
