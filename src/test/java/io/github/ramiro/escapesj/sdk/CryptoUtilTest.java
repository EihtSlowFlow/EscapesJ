package io.github.ramiro.escapesj.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.*;

public class CryptoUtilTest {

    @Test
    public void testCifrarYDescifrar() throws Exception {
        String original = "MensajeSuperSecreto123";
        String cifrado = CryptoUtil.encrypt(original);

        assertNotNull(cifrado);
        assertNotEquals(original, cifrado);

        String descifrado = CryptoUtil.decrypt(cifrado);
        assertEquals(original, descifrado);
    }

    @Test
    public void testFailFastCuandoCifradoInvalido() {
        assertThrows(RuntimeException.class, () -> {
            CryptoUtil.decrypt("TokenInvalidoQueNoEsBase64");
        });
    }
}
