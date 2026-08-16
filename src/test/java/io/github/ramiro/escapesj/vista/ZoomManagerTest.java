package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.persistencia.ConfigRepository;
import io.github.ramiro.escapesj.persistencia.DatabaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
        ZoomManager.setPersistenceErrorHandler(error -> { });
        ZoomManager.restablecer(); // Asegurar 100% para evitar afectar otros tests
        SwingUtilities.invokeAndWait(() -> { });
        ZoomManager.setPersistenceErrorHandler(null);
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

    @Test
    public void testLimitesConValorPersistidoNoAlineadoAlPaso() {
        configRepo.guardar("ui.scale_percent", "195");
        ZoomManager.inicializar(configRepo);
        ZoomManager.aumentar();
        assertEquals(200, ZoomManager.getScalePercent());

        configRepo.guardar("ui.scale_percent", "85");
        ZoomManager.inicializar(configRepo);
        ZoomManager.reducir();
        assertEquals(80, ZoomManager.getScalePercent());
    }

    @Test
    public void testReinicializarAlCienRestauraAlturaBase() {
        configRepo.guardar("ui.scale_percent", "120");
        ZoomManager.inicializar(configRepo);
        assertEquals(19, UIManager.getDefaults().getInt("Table.rowHeight"));

        configRepo.guardar("ui.scale_percent", "100");
        ZoomManager.inicializar(configRepo);
        assertEquals(16, UIManager.getDefaults().getInt("Table.rowHeight"));
    }

    @Test
    public void testEscalaComponentesSwingConValoresExplicitos() {
        configRepo.guardar("ui.scale_percent", "100");
        ZoomManager.inicializar(configRepo);

        JPanel panel = new JPanel();
        JButton button = new JButton("Guardar");
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(100, 30));
        JTable table = new JTable(2, 2);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(button);
        panel.add(table);

        ZoomManager.aumentar();
        ZoomManager.applyScaleToTree(panel);

        assertEquals(14.3f, button.getFont().getSize2D(), 0.01f);
        assertEquals(new Dimension(110, 33), button.getPreferredSize());
        assertEquals(33, table.getRowHeight());
        assertEquals(13.2f, table.getTableHeader().getFont().getSize2D(), 0.01f);

        ZoomManager.aumentar();
        ZoomManager.applyScaleToTree(panel);
        assertEquals(15.6f, button.getFont().getSize2D(), 0.01f);
        assertEquals(36, table.getRowHeight());
    }

    @Test
    public void testFalloDePersistenciaNoImpideAplicarZoom() throws Exception {
        AtomicInteger reportedErrors = new AtomicInteger();
        ZoomManager.setPersistenceErrorHandler(error -> reportedErrors.incrementAndGet());
        ConfigRepository failingRepository = new ConfigRepository() {
            @Override
            public Optional<String> obtener(String clave) {
                return Optional.of("100");
            }

            @Override
            public void guardar(String clave, String valor) {
                throw new io.github.ramiro.escapesj.persistencia.PersistenceException("fallo simulado");
            }
        };
        ZoomManager.inicializar(failingRepository);

        assertDoesNotThrow(ZoomManager::aumentar);
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(110, ZoomManager.getScalePercent());
        assertEquals(1, reportedErrors.get());
    }
}
