package io.github.ramiro.escapesj.sdk;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        if (valor == null) {
            return 0L;
        }
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
}
