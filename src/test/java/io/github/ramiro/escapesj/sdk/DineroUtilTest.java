package io.github.ramiro.escapesj.sdk;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class DineroUtilTest {

    @Test
    public void testParsearMontoArs_Validos() {
        assertEquals(new BigDecimal("3000000"), DineroUtil.parsearMontoArs("3000000"));
        assertEquals(new BigDecimal("3000000"), DineroUtil.parsearMontoArs("3.000.000"));
        assertEquals(new BigDecimal("15000.50"), DineroUtil.parsearMontoArs("15000,50"));
        assertEquals(new BigDecimal("15000.50"), DineroUtil.parsearMontoArs("15.000,50"));
        assertEquals(new BigDecimal("15000.50"), DineroUtil.parsearMontoArs("$ 15.000,50"));
        assertEquals(new BigDecimal("15000.50"), DineroUtil.parsearMontoArs("  15000.50  "));
        
        // Single dot treated as decimal
        assertEquals(new BigDecimal("15.000"), DineroUtil.parsearMontoArs("15.000"));
        assertEquals(new BigDecimal("1.50"), DineroUtil.parsearMontoArs("1.50"));
    }

    @Test
    public void testParsearMontoArs_Invalidos() {
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs("1,2,3"));
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs("15..000"));
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs("texto"));
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs(""));
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs("   "));
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs(null));
        
        // Starts with dot or comma without leading digits is blocked by the regex
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs(".50"));
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs(",50"));
        
        // Two commas
        assertThrows(IllegalArgumentException.class, () -> DineroUtil.parsearMontoArs("15,000,50"));
    }
}
