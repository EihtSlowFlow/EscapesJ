package io.github.ramiro.escapesj.persistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Files;
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

    @Test
    public void testRutasPorDefectoPortables() {
        Path documents = tempDir.resolve("Documentos");
        assertDoesNotThrow(() -> Files.createDirectory(documents));

        assertEquals(documents.toAbsolutePath().toString(),
                ConfigRepository.resolverDefaultDocumentsPath(documents.toFile(), tempDir.toString()));
        assertEquals(tempDir.resolve("Documents").toAbsolutePath().normalize().toString(),
                ConfigRepository.resolverDefaultDocumentsPath(null, tempDir.toString()));
    }

    @Test
    public void testRestablecerRutaGuardandoVacio() {
        // Simulamos que el usuario tenía una ruta personalizada
        configRepo.guardar("ruta.boletas", "C:\\Custom\\Path");
        assertEquals("C:\\Custom\\Path", configRepo.getRutaBoletas());
        
        // Ahora simulamos que el usuario la borra desde la UI y guarda un string vacío
        configRepo.guardar("ruta.boletas", "");
        
        // Debe retornar el default, ya que el string vacío se interpreta como ausencia de configuración
        assertNotEquals("C:\\Custom\\Path", configRepo.getRutaBoletas());
        assertEquals(ConfigRepository.getDefaultBoletasPath(), configRepo.getRutaBoletas());
    }

    @Test
    public void testGuardarRutasDefaultEliminaOverrides() {
        configRepo.guardar("ruta.boletas", "/ruta/personalizada/boletas");
        configRepo.guardar("ruta.presupuestos", "/ruta/personalizada/presupuestos");

        configRepo.guardarRutas(
                ConfigRepository.getDefaultBoletasPath(),
                ConfigRepository.getDefaultPresupuestosPath());

        assertTrue(configRepo.obtener("ruta.boletas").isEmpty());
        assertTrue(configRepo.obtener("ruta.presupuestos").isEmpty());
        assertEquals(ConfigRepository.getDefaultBoletasPath(), configRepo.getRutaBoletas());
        assertEquals(ConfigRepository.getDefaultPresupuestosPath(), configRepo.getRutaPresupuestos());
    }

    @Test
    public void testGuardarRutasConservaOverridesPersonalizados() {
        Path boletas = tempDir.resolve("salida").resolve("..").resolve("boletas").toAbsolutePath();
        Path presupuestos = tempDir.resolve("presupuestos").toAbsolutePath();
        configRepo.guardarRutas(" " + boletas + " ", " " + presupuestos + " ");

        assertEquals(boletas.normalize().toString(), configRepo.getRutaBoletas());
        assertEquals(presupuestos.normalize().toString(), configRepo.getRutaPresupuestos());
        assertTrue(Files.isDirectory(boletas.normalize()));
        assertTrue(Files.isDirectory(presupuestos));
    }

    @Test
    public void testGuardarRutasRechazaRutaRelativaYArchivo() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> configRepo.guardarRutas("relativa/boletas", tempDir.resolve("presupuestos").toString()));

        Path archivo = Files.createFile(tempDir.resolve("archivo.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> configRepo.guardarRutas(archivo.toString(), tempDir.resolve("presupuestos").toString()));
    }
}
