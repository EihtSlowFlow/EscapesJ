package io.github.ramiro.escapesj.persistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigRepositoryTest {

    private ConfigRepository configRepo;

    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Path db = tempDir.resolve("escapesj-test-config.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + db.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();
        configRepo = new ConfigRepository();
    }

    @AfterEach
    public void tearDown() throws Exception {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    public void testGuardarMultiples_RollbackOnNullValue() {
        // Configuraciones preexistentes
        configRepo.guardar("clave1", "valorOriginal1");
        
        Map<String, String> configs = new LinkedHashMap<>();
        configs.put("clave1", "valorNuevo1"); // Debería cambiar
        configs.put("clave2", "valorNuevo2"); // Debería insertar
        configs.put("clave3", null); // Fallará porque valor no puede ser null en SQLite
        
        Exception exception = assertThrows(PersistenceException.class, () -> {
            configRepo.guardarMultiples(configs);
        });
        
        // Validar que clave1 no cambió y clave2 no se insertó debido al rollback
        assertEquals("valorOriginal1", configRepo.obtener("clave1").orElse(null));
        assertTrue(configRepo.obtener("clave2").isEmpty());
    }
}
