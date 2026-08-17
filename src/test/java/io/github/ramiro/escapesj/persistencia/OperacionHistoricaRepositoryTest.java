package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.OperacionHistorica;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class OperacionHistoricaRepositoryTest {

    @TempDir
    Path tempDir;

    private OperacionHistoricaRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-operaciones.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + dbPath.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();

        repository = new OperacionHistoricaRepository();
    }

    @AfterEach
    void tearDown() {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    void testCRUDCompleto() throws Exception {
        // Create
        OperacionHistorica op = new OperacionHistorica(0, "2026-08-10", "REF-001", "Cliente Prueba", "Venta repuestos", new BigDecimal("1500.00"), new BigDecimal("1000.00"), "Obs", "PENDIENTE", null, "", "");
        repository.guardar(op);
        
        var guardadaOpt = repository.buscarTodas().stream().findFirst();
        assertTrue(guardadaOpt.isPresent());
        OperacionHistorica guardada = guardadaOpt.get();
        assertEquals("Cliente Prueba", guardada.getCliente());
        assertEquals(new BigDecimal("1500.00"), guardada.getImporteTotal());
        assertEquals("PENDIENTE", guardada.getEstado());
        
        // Update
        guardada.setCliente("Cliente Editado");
        guardada.setEstado("DIGITALIZADO");
        repository.guardar(guardada);
        
        var actualizadaOpt = repository.buscarPorId(guardada.getId());
        assertTrue(actualizadaOpt.isPresent());
        assertEquals("Cliente Editado", actualizadaOpt.get().getCliente());
        assertEquals("DIGITALIZADO", actualizadaOpt.get().getEstado());
        
        // Delete
        repository.eliminar(guardada.getId());
        var borradaOpt = repository.buscarPorId(guardada.getId());
        assertFalse(borradaOpt.isPresent());
    }

    @Test
    void testActualizarInexistenteFalla() {
        OperacionHistorica op = new OperacionHistorica(999, "2026-08-10", "REF-001", "C", "D", BigDecimal.ZERO, BigDecimal.ZERO, "", "PENDIENTE", null, "", "");
        assertThrows(PersistenceException.class, () -> repository.guardar(op));
    }

    @Test
    void testEliminarInexistenteFalla() {
        assertThrows(PersistenceException.class, () -> repository.eliminar(999));
    }
}
