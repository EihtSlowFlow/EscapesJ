package io.github.ramiro.escapesj.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recomendado para GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits tag
    private static final String KEY_FILE_PATH = System.getProperty("user.home") + File.separator + ".escapesj" + File.separator + "escapesj_master.key";

    private static SecretKey masterKey;

    static {
        try {
            initMasterKey();
        } catch (Exception e) {
            logger.error("Advertencia crítica: Error inicializando CryptoUtil: " + e.getMessage(), e);
        }
    }

    private static void setRestrictivePermissions(Path path, boolean isDir) throws IllegalStateException {
        try {
            if (isDir) {
                Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
            } else {
                Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            }
        } catch (UnsupportedOperationException e) {
            File file = path.toFile();
            boolean ok = true;
            ok &= file.setReadable(false, false);
            ok &= file.setWritable(false, false);
            ok &= file.setExecutable(false, false);
            ok &= file.setReadable(true, true);
            ok &= file.setWritable(true, true);
            if (isDir) ok &= file.setExecutable(true, true);
            if (!ok) {
                logger.error("No se pudieron establecer los permisos restrictivos de fallback (Windows) en: " + path);
            }
        } catch (Exception e) {
            logger.error("Fallo de seguridad: No se pudieron establecer permisos restrictivos seguros en " + path + ". Asegúrese de no ejecutar esto en un entorno expuesto.", e);
        }
    }

    private static void initMasterKey() throws Exception {
        Path keyPath = Path.of(KEY_FILE_PATH);
        Path parent = keyPath.getParent();
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        setRestrictivePermissions(parent, true);

        if (Files.exists(keyPath)) {
            byte[] keyBytes = Files.readAllBytes(keyPath);
            masterKey = new SecretKeySpec(keyBytes, "AES");
            setRestrictivePermissions(keyPath, false);
        } else {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            masterKey = keyGen.generateKey();

            Files.write(keyPath, masterKey.getEncoded());
            setRestrictivePermissions(keyPath, false);
            logger.info("CryptoUtil: Clave maestra generada en " + KEY_FILE_PATH);
        }
    }

    /**
     * Cifra un texto plano usando AES-GCM.
     * Retorna un string en formato Base64(IV + CipherText).
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        if (masterKey == null) throw new IllegalStateException("CryptoUtil no inicializado correctamente. Clave maestra nula.");

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
            logger.error("CryptoUtil: Error al cifrar: " + e.getMessage());
            throw new RuntimeException("Fallo crítico de encriptación", e);
        }
    }

    /**
     * Descifra un string cifrado en formato Base64(IV + CipherText).
     */
    public static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) return encryptedBase64;
        if (masterKey == null) throw new IllegalStateException("CryptoUtil no inicializado correctamente. Clave maestra nula.");

        try {
            // Intento de descifrar (si no es Base64 válido o es muy corto, fallará)
            byte[] ivAndCipherText = Base64.getDecoder().decode(encryptedBase64);
            if (ivAndCipherText.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("El texto no tiene el tamaño mínimo para contener un IV de GCM");
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
            // No era Base64, consideramos que es un texto corrupto o no cifrado en absoluto
            throw new RuntimeException("El token almacenado no es un texto cifrado válido.", e);
        } catch (Exception e) {
            logger.error("CryptoUtil: Error al descifrar: " + e.getMessage());
            throw new RuntimeException("Fallo crítico de desencriptación", e);
        }
    }
}
