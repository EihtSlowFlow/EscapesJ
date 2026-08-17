package io.github.ramiro.escapesj.persistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoletaRepositoryTest {

    @TempDir
    Path tempDir;

    private BoletaRepository boletaRepository;

    @BeforeEach
    void setUp() throws Exception {
        Path db = tempDir.resolve("escapesj-test.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + db.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();
        boletaRepository = new BoletaRepository();
    }

    @AfterEach
    void tearDown() {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    void testCrearBoletaYAgregarItem() {
        int boletaId = boletaRepository.crearBoleta("12345678", "Juan Perez", "2026-08-16", new BigDecimal("1500.00"));
        assertTrue(boletaId > 0);

        boletaRepository.agregarItem(boletaId, "PRODUCTO", "Caño", "COD-1", 2, new BigDecimal("750.00"), new BigDecimal("500.00"));

        List<BoletaRepository.BoletaResumen> boletas = boletaRepository.buscarBoletasPorDni("12345678");
        assertEquals(1, boletas.size());
        assertEquals("Juan Perez", boletas.get(0).nombreCliente());
        assertEquals(0, new BigDecimal("1500.00").compareTo(boletas.get(0).total()));

        List<BoletaRepository.BoletaItem> items = boletaRepository.obtenerItems(boletaId);
        assertEquals(1, items.size());
        assertEquals("Caño", items.get(0).descripcion());
        assertEquals(2, items.get(0).cantidad());
        assertEquals(0, new BigDecimal("1500.00").compareTo(items.get(0).subtotal()));
    }
}
