package io.github.ramiro.escapesj.sdk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Utilidad centralizada para conversiones de dinero y centavos de precisión.
 */
public class DineroUtil {

    private DineroUtil() {} // Utility class

    /**
     * Convierte un monto BigDecimal a un valor long en centavos.
     * Si el valor de entrada tiene más de dos decimales, se lanzará ArithmeticException.
     */
    public static long aCentavos(BigDecimal valor) {
        Objects.requireNonNull(valor, "El importe no puede ser null.");
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los importes negativos no están permitidos.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
    }

    /**
     * Convierte un valor en centavos (long) a un monto BigDecimal.
     */
    public static BigDecimal desdeCentavos(long valor) {
        return BigDecimal.valueOf(valor, 2);
    }

    /**
     * Redondea explícitamente un cálculo interno a 2 decimales.
     */
    public static BigDecimal redondearMoneda(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Parsea un string ingresado por el usuario en formato argentino a BigDecimal.
     */
    public static BigDecimal parsearMontoArs(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El importe no puede estar vacío.");
        }
        String limpio = texto.replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El importe no puede estar vacío.");
        }

        long countCommas = limpio.chars().filter(ch -> ch == ',').count();
        long countDots = limpio.chars().filter(ch -> ch == '.').count();

        String parseable;

        if (countDots > 0 && countCommas == 1) {
            if (!limpio.matches("^\\d{1,3}(\\.\\d{3})*,\\d+$")) {
                throw new IllegalArgumentException("Formato inválido.");
            }
            parseable = limpio.replace(".", "").replace(",", ".");
        } else if (countDots > 1 && countCommas == 0) {
            if (!limpio.matches("^\\d{1,3}(\\.\\d{3})+$")) {
                throw new IllegalArgumentException("Formato inválido.");
            }
            parseable = limpio.replace(".", "");
        } else if (countCommas == 1 && countDots == 0) {
            if (!limpio.matches("^\\d+,\\d+$")) {
                throw new IllegalArgumentException("Formato inválido.");
            }
            parseable = limpio.replace(",", ".");
        } else if (countDots == 1 && countCommas == 0) {
            if (!limpio.matches("^\\d+\\.\\d+$")) {
                throw new IllegalArgumentException("Formato inválido.");
            }
            parseable = limpio;
        } else if (countDots == 0 && countCommas == 0) {
            if (!limpio.matches("^\\d+$")) {
                throw new IllegalArgumentException("Formato inválido.");
            }
            parseable = limpio;
        } else {
            throw new IllegalArgumentException("El importe tiene un formato numérico inválido.");
        }

        try {
            return new BigDecimal(parseable);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El importe no tiene un formato numérico válido.");
        }
    }
}
