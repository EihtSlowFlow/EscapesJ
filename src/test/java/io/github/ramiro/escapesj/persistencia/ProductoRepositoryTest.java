package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.Producto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductoRepositoryTest {

    @TempDir
    Path tempDir;

    private ProductoRepository productoRepository;

    @BeforeEach
    void setUp() throws Exception {
        Path db = tempDir.resolve("escapesj-test.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + db.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();
        productoRepository = new ProductoRepository();
    }

    @AfterEach
    void tearDown() {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    void testGuardarYBuscarPorCodigo() {
        Producto p = new Producto("COD-123", "Caño Escape", "Descripción", new BigDecimal("1500.00"), 5);
        productoRepository.guardar(p);

        Optional<Producto> buscado = productoRepository.buscarPorCodigo("COD-123");
        assertTrue(buscado.isPresent());
        assertEquals("Caño Escape", buscado.get().getNombre());
        assertEquals(0, new BigDecimal("1500.00").compareTo(buscado.get().getPrecio()));
        assertEquals(5, buscado.get().getStock());
    }

    @Test
    void testActualizarConCambioDeCodigo() {
        Producto p = new Producto("COD-OLD", "Caño", "Desc", new BigDecimal("1000.00"), 2);
        productoRepository.guardar(p);

        Producto modificado = new Producto("COD-NEW", "Caño Modificado", "Desc 2", new BigDecimal("1200.00"), 3);
        productoRepository.actualizarConCambioDeCodigo(modificado, "COD-OLD");

        Optional<Producto> viejo = productoRepository.buscarPorCodigo("COD-OLD");
        assertFalse(viejo.isPresent());

        Optional<Producto> nuevo = productoRepository.buscarPorCodigo("COD-NEW");
        assertTrue(nuevo.isPresent());
        assertEquals("Caño Modificado", nuevo.get().getNombre());
    }

    @Test
    void testRestarStock() {
        Producto p = new Producto("STK-01", "Stock", "Desc", new BigDecimal("100"), 10);
        productoRepository.guardar(p);

        boolean resultado1 = productoRepository.intentarRestarStock("STK-01", 3);
        assertTrue(resultado1);

        Optional<Producto> post1 = productoRepository.buscarPorCodigo("STK-01");
        assertTrue(post1.isPresent());
        assertEquals(7, post1.get().getStock());

        // Intentar restar más del disponible
        boolean resultado2 = productoRepository.intentarRestarStock("STK-01", 10);
        assertFalse(resultado2);
        
        Optional<Producto> post2 = productoRepository.buscarPorCodigo("STK-01");
        assertTrue(post2.isPresent());
        assertEquals(7, post2.get().getStock());
    }
}
