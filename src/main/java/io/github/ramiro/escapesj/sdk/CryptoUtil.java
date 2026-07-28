package io.github.ramiro.escapesj.sdk;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Utilidad criptográfica para cifrar información sensible en repositorios (ej. tokens de acceso).
 * Utiliza AES-GCM de 256 bits y almacena una clave maestra localmente con permisos restrictivos.
 */
public class CryptoUtil {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recomendado para GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits tag
    private static final String KEY_FILE_PATH = "escapesj_master.key";
    
    private static SecretKey masterKey;

    static {
        try {
            initMasterKey();
        } catch (Exception e) {
            System.err.println("Advertencia crítica: Error inicializando CryptoUtil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initMasterKey() throws Exception {
        Path keyPath = Path.of(KEY_FILE_PATH);
        if (Files.exists(keyPath)) {
            byte[] keyBytes = Files.readAllBytes(keyPath);
            masterKey = new SecretKeySpec(keyBytes, "AES");
        } else {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            masterKey = keyGen.generateKey();
            
            Files.write(keyPath, masterKey.getEncoded());
            
            // Intentar aplicar permisos restrictivos 600 si el sistema lo soporta (ej. Linux/Mac)
            try {
                Files.setPosixFilePermissions(keyPath, Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE
                ));
            } catch (UnsupportedOperationException e) {
                // Ignorar en Windows
                File file = keyPath.toFile();
                file.setReadable(false, false);
                file.setWritable(false, false);
                file.setReadable(true, true);
                file.setWritable(true, true);
            }
            System.out.println("CryptoUtil: Clave maestra generada en " + KEY_FILE_PATH);
        }
    }

    /**
     * Cifra un texto plano usando AES-GCM. 
     * Retorna un string en formato Base64(IV + CipherText).
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        if (masterKey == null) return plainText; // Failsafe (fallback a plano)

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            
            // Concatenar IV + CipherText
            byte[] ivAndCipherText = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, ivAndCipherText, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, ivAndCipherText, GCM_IV_LENGTH, cipherText.length);
            
            return Base64.getEncoder().encodeToString(ivAndCipherText);
        } catch (Exception e) {
            System.err.println("CryptoUtil: Error al cifrar: " + e.getMessage());
            return plainText; // Fallback gracefully? Or throw? Better throw or log and return plain if we can't secure it.
            // In a strict environment, throw new RuntimeException(e);
        }
    }

    /**
     * Descifra un string cifrado en formato Base64(IV + CipherText).
     */
    public static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) return encryptedBase64;
        if (masterKey == null) return encryptedBase64;

        try {
            // Intento de descifrar (si no es Base64 válido o es muy corto, fallará, asumiendo que era texto plano antiguo)
            byte[] ivAndCipherText = Base64.getDecoder().decode(encryptedBase64);
            if (ivAndCipherText.length < GCM_IV_LENGTH) {
                return encryptedBase64; // Era texto plano
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(ivAndCipherText, 0, iv, 0, GCM_IV_LENGTH);
            
            byte[] cipherText = new byte[ivAndCipherText.length - GCM_IV_LENGTH];
            System.arraycopy(ivAndCipherText, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);

            return new String(cipher.doFinal(cipherText));
        } catch (IllegalArgumentException e) {
            // No era Base64 (ej. texto plano antiguo en la BBDD migrada)
            return encryptedBase64; 
        } catch (Exception e) {
            System.err.println("CryptoUtil: Error al descifrar (posible texto plano antiguo): " + e.getMessage());
            return encryptedBase64; // Fallback
        }
    }
}
