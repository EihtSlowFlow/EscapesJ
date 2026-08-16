package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.persistencia.ConfigRepository;
import io.github.ramiro.escapesj.persistencia.DatabaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ZoomManagerTest {

    @TempDir
    Path tempDir;

    private ConfigRepository configRepo;

    @BeforeEach
    public void setUp() throws Exception {
        Path db = tempDir.resolve("zoom-test.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + db.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();
        configRepo = new ConfigRepository();
        UIManager.getDefaults().put("Table.rowHeight", 16);
    }

    @AfterEach
    public void tearDown() throws Exception {
        ZoomManager.restablecer(); // Asegurar 100% para evitar afectar otros tests
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    public void testLimitesYPersistencia() {
        // Reiniciar
        configRepo.guardar("ui.scale_percent", "100");
        ZoomManager.inicializar(configRepo);
        assertEquals(100, ZoomManager.getScalePercent());

        // Aumentar hasta el máximo
        for (int i = 0; i < 20; i++) ZoomManager.aumentar();
        assertEquals(200, ZoomManager.getScalePercent());
        assertEquals("200", configRepo.obtener("ui.scale_percent").orElse(""));

        // Reducir hasta el mínimo
        for (int i = 0; i < 20; i++) ZoomManager.reducir();
        assertEquals(80, ZoomManager.getScalePercent());
        assertEquals("80", configRepo.obtener("ui.scale_percent").orElse(""));
    }

    @Test
    public void testRestablecer() {
        ZoomManager.inicializar(configRepo);
        ZoomManager.aumentar();
        assertNotEquals(100, ZoomManager.getScalePercent());
        ZoomManager.restablecer();
        assertEquals(100, ZoomManager.getScalePercent());
    }

    @Test
    public void testValoresCorruptos() {
        configRepo.guardar("ui.scale_percent", "asdf");
        ZoomManager.inicializar(configRepo);
        assertEquals(100, ZoomManager.getScalePercent());

        configRepo.guardar("ui.scale_percent", "300"); // Fuera de rango
        ZoomManager.inicializar(configRepo);
        assertEquals(100, ZoomManager.getScalePercent());

        configRepo.guardar("ui.scale_percent", "50"); // Fuera de rango
        ZoomManager.inicializar(configRepo);
        assertEquals(100, ZoomManager.getScalePercent());
    }

    @Test
    public void testNoAcumulativoYAlturaDeTabla() {
        // Forzamos un reset del ZoomManager
        configRepo.guardar("ui.scale_percent", "100");
        ZoomManager.inicializar(configRepo);
        
        int baseHeight = 16;
        UIManager.getDefaults().put("Table.rowHeight", baseHeight);

        // Simular que el usuario hace zoom
        ZoomManager.aumentar(); // 110%
        int expected110 = (int) (baseHeight * 1.1f);
        assertEquals(expected110, UIManager.getDefaults().getInt("Table.rowHeight"));

        ZoomManager.aumentar(); // 120%
        int expected120 = (int) (baseHeight * 1.2f);
        assertEquals(expected120, UIManager.getDefaults().getInt("Table.rowHeight"));
    }
}
