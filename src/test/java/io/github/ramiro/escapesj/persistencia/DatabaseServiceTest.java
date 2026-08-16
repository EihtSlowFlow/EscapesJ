package io.github.ramiro.escapesj.persistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseServiceTest {

    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Path db = tempDir.resolve("escapesj-test-db.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + db.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();
    }

    @AfterEach
    public void tearDown() throws Exception {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    public void testGetConnection_FalloConexionLanzaPersistenceException() {
        // Forzamos una URL inválida (por ejemplo, sin jdbc:sqlite:)
        DatabaseService.setCustomDbUrl("jdbc:sqlite:/ruta/invalida/db/que/no/existe.db");
        
        Exception exception = assertThrows(PersistenceException.class, () -> {
            DatabaseService.getConnection();
        });
        
        assertTrue(exception.getMessage().contains("No se pudo establecer la conexión") || 
                   exception.getCause() instanceof SQLException);
    }

    @Test
    public void testOperacionSobreTablaEliminada_LanzaPersistenceException() {
        // Simulamos la eliminación de la tabla usuarios
        try (Connection conn = DatabaseService.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE usuarios");
        } catch (SQLException e) {
            fail("Falló la preparación del test");
        }
        
        UsuarioRepository repo = new UsuarioRepository();
        
        // Ahora una operación sobre usuarios debe lanzar PersistenceException
        Exception exception = assertThrows(PersistenceException.class, () -> {
            repo.isUsuariosEmpty();
        });
        
        assertTrue(exception.getCause() instanceof SQLException);
    }
}
