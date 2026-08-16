package io.github.ramiro.escapesj.persistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class UsuarioRepositoryTest {

    private UsuarioRepository usuarioRepo;

    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Path db = tempDir.resolve("escapesj-test-usuario.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + db.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();
        usuarioRepo = new UsuarioRepository();
        usuarioRepo.crearAdminSetupInicial("admin", "admin123");
    }

    @AfterEach
    public void tearDown() throws Exception {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    public void testActualizarCredenciales_LanzaExceptionSiUsuarioNoExiste() {
        Exception exception = assertThrows(PersistenceException.class, () -> {
            usuarioRepo.actualizarCredenciales("noexiste", "nuevouser", "nuevopass", null, null);
        });
        assertTrue(exception.getMessage().contains("Error actualizando credenciales"));
        assertTrue(exception.getCause().getMessage().contains("No se encontró el usuario actual para actualizar el nombre"));
    }

    @Test
    public void testActualizarCredenciales_RollbackOnSqlException() throws Exception {
        // Creamos un trigger que tire un error cuando intenten actualizar la contraseña
        try (Connection conn = DatabaseService.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TRIGGER prevent_password_update " +
                         "BEFORE UPDATE OF password ON usuarios " +
                         "BEGIN SELECT RAISE(ABORT, 'Simulated SQL Exception'); END;");
        }
        
        // Intentamos cambiar el nombre de "admin" a "nuevo_admin" y su contraseña.
        // La primera operación (cambiar nombre) funciona, pero la segunda (cambiar password)
        // activará el trigger y lanzará una SQLException, lo cual forzará un rollback completo.
        Exception exception = assertThrows(PersistenceException.class, () -> {
            usuarioRepo.actualizarCredenciales("admin", "nuevo_admin", "nuevopass", "preg", "resp");
        });
        
        // Validar que la operación entera se deshizo:
        // 1. "admin" todavía existe y su contraseña no cambió
        assertTrue(usuarioRepo.validarCredenciales("admin", "admin123"), "El usuario admin debería seguir existiendo con su contraseña original");
        
        // 2. "nuevo_admin" no se creó
        assertFalse(usuarioRepo.validarCredenciales("nuevo_admin", "nuevopass"), "El usuario nuevo_admin no debería existir");
    }
}
