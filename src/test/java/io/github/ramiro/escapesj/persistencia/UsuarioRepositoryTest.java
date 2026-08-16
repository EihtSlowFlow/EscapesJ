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
        // Rompemos la tabla de usuarios intencionalmente (ej. quitamos la columna pregunta_seguridad)
        // O más fácil: provocamos un error SQL mandando null a una columna NOT NULL en SQLite.
        // En nuestro esquema, "usuario" es PRIMARY KEY, pero podemos probar un error SQL eliminando la tabla en medio, o pasando null a respuesta_seguridad si es NOT NULL.
        // Pero como respuesta_seguridad no es NOT NULL (admite null según la migración inicial? no, es texto normal).
        // Vamos a romper la tabla usuarios tirando la columna 'password' si SQLite lo permitiera, pero SQLite no deja.
        // Solución: forzamos SQLException haciendo DROP TABLE usuarios en medio de la transacción? No podemos fácilmente inyectarlo.
        // Usemos un nombre de usuario null para ver si lanza constraint violation, pero BCrypt lanza error con password null antes.
        
        // Mejor: renombramos la tabla para que el segundo UPDATE falle. 
        // En realidad podemos simplemente usar el test anterior para ver si se hizo rollback parcial, 
        // pero la atomicidad se garantiza por TransactionHelper. 
        // Vamos a insertar un usuario con constraint UNIQUE repetida para usuarioNuevo:
        usuarioRepo.crearAdminSetupInicial("otro", "admin123");
        
        // Intentamos renombrar "admin" a "otro", lo cual viola UNIQUE
        Exception exception = assertThrows(PersistenceException.class, () -> {
            usuarioRepo.actualizarCredenciales("admin", "otro", "nuevopass", "preg", "resp");
        });
        
        // Validar que la contraseña de admin NO cambió
        assertTrue(usuarioRepo.validarCredenciales("admin", "admin123"));
    }
}
