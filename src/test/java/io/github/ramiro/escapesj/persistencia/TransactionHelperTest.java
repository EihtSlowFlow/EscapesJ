package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.servicio.FacturacionRequest;
import io.github.ramiro.escapesj.servicio.FacturacionService;
import io.github.ramiro.escapesj.servicio.ItemFacturacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionHelperTest {

    private Connection conn;
    private BoletaRepository boletaRepo;
    private ProductoRepository productoRepo;
    private ServicioRepository servicioRepo;
    private FacturacionService facturacionService;

    public TransactionHelperTest() {
    }

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path tempDir;

    @org.junit.jupiter.api.BeforeEach
    public void setUp() throws Exception {
        java.nio.file.Path db = tempDir.resolve("escapesj-test.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + db.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    private int countRows(String sql) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Test
    public void testTransactionRollbacksOnError() throws Exception {
        conn = DatabaseService.getConnection();
        boletaRepo = new BoletaRepository();
        productoRepo = new ProductoRepository();
        servicioRepo = new ServicioRepository();
        facturacionService = new FacturacionService(boletaRepo, productoRepo, servicioRepo);

        productoRepo.guardar(new Producto("TEST-001", "Producto de Prueba", "Test", BigDecimal.valueOf(1000.0), 10));
        productoRepo.guardar(new Producto("TEST-002", "Producto Sin Stock", "Test", BigDecimal.valueOf(1000.0), 1)); // We will ask for 5

        int initialBoletas = countRows("SELECT COUNT(*) FROM boletas");
        int initialItems = countRows("SELECT COUNT(*) FROM boleta_items");
        int initialServicios = countRows("SELECT COUNT(*) FROM servicios_historial");

        FacturacionRequest request = new FacturacionRequest(
                "11111111", "Test Rollback Cliente", "2026-01-01",
                List.of(
                        new ItemFacturacion("PRODUCTO", "P1", "TEST-001", 2, new java.math.BigDecimal("1000")), // This one succeeds
                        new ItemFacturacion("PRODUCTO", "P2", "TEST-002", 5, new java.math.BigDecimal("1000"))  // This one will fail due to no stock
                ), "EFECTIVO", java.math.BigDecimal.ZERO
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            facturacionService.facturarOrden(request);
        });

        assertTrue(exception.getMessage().contains("Stock insuficiente para el producto: P2"));

        // Verify that the rollback was successful across all 4 tables

        // 1. Check stock is back to 10 for TEST-001
        var productoOpt = productoRepo.buscarPorCodigo("TEST-001");
        assertTrue(productoOpt.isPresent());
        assertEquals(10, productoOpt.get().getStock(), "Stock should be rolled back to 10 for first product");

        // 2. Check that boleta count is identical
        assertEquals(initialBoletas, countRows("SELECT COUNT(*) FROM boletas"));
        assertEquals(initialItems, countRows("SELECT COUNT(*) FROM boleta_items"));
        assertEquals(initialServicios, countRows("SELECT COUNT(*) FROM servicios_historial"));
    }

    @Test
    public void testTransactionCommitsOnSuccess() throws Exception {
        conn = DatabaseService.getConnection();
        boletaRepo = new BoletaRepository();
        productoRepo = new ProductoRepository();
        servicioRepo = new ServicioRepository();
        facturacionService = new FacturacionService(boletaRepo, productoRepo, servicioRepo);

        productoRepo.guardar(new Producto("TEST-001", "Producto de Prueba", "Test", BigDecimal.valueOf(1000.0), 10));

        int initialBoletas = countRows("SELECT COUNT(*) FROM boletas WHERE nombre_cliente = 'Test Commit Cliente'");
        int initialItems = countRows("SELECT COUNT(*) FROM boleta_items WHERE descripcion = 'P1'");
        int initialServicios = countRows("SELECT COUNT(*) FROM servicios_historial WHERE nombre = 'Test Commit Cliente'");

        FacturacionRequest request = new FacturacionRequest(
                "11111111", "Test Commit Cliente", "2026-01-01",
                List.of(
                        new ItemFacturacion("PRODUCTO", "P1", "TEST-001", 5, new java.math.BigDecimal("1000"))
                ), "TRANSFERENCIA", java.math.BigDecimal.ZERO
        );

        var result = facturacionService.facturarOrden(request);

        // Verify that the commit was successful
        assertTrue(result.boletaId() > 0);

        // 1. Check stock is now 5
        var productoOpt = productoRepo.buscarPorCodigo("TEST-001");
        assertTrue(productoOpt.isPresent());
        assertEquals(5, productoOpt.get().getStock(), "Stock should be committed to 5");

        // 2. Check boleta and history created
        assertEquals(initialBoletas + 1, countRows("SELECT COUNT(*) FROM boletas WHERE nombre_cliente = 'Test Commit Cliente'"));
        assertEquals(initialServicios + 1, countRows("SELECT COUNT(*) FROM servicios_historial WHERE nombre = 'Test Commit Cliente'"));
    }
    public void testTransactionRollbacksOnSqlException() throws Exception {
        conn = DatabaseService.getConnection();
        boletaRepo = new BoletaRepository();
        productoRepo = new ProductoRepository();
        servicioRepo = new ServicioRepository();
        facturacionService = new FacturacionService(boletaRepo, productoRepo, servicioRepo);

        productoRepo.guardar(new Producto("TEST-003", "Producto Sql", "Test", BigDecimal.valueOf(1000.0), 10));

        int initialBoletas = countRows("SELECT COUNT(*) FROM boletas");
        int initialItems = countRows("SELECT COUNT(*) FROM boleta_items");
        int initialServicios = countRows("SELECT COUNT(*) FROM servicios_historial");

        // Romperemos la tabla boleta_items para que lance SQLException durante la facturación (después de restar el stock)
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE boleta_items");
        }

        FacturacionRequest request = new FacturacionRequest(
                "11111111", "Test Rollback SQL", "2026-01-01",
                List.of(
                        new ItemFacturacion("PRODUCTO", "P3", "TEST-003", 2, new java.math.BigDecimal("1000"))
                ), "EFECTIVO", java.math.BigDecimal.ZERO
        );

        Exception exception = assertThrows(PersistenceException.class, () -> {
            facturacionService.facturarOrden(request);
        });

        // Verify that the rollback was successful for the stock deduction
        var productoOpt = productoRepo.buscarPorCodigo("TEST-003");
        assertTrue(productoOpt.isPresent());
        assertEquals(10, productoOpt.get().getStock(), "Stock should be rolled back to 10 after SQL error");

        // And other tables are unchanged
        assertEquals(initialBoletas, countRows("SELECT COUNT(*) FROM boletas"));
        assertEquals(initialServicios, countRows("SELECT COUNT(*) FROM servicios_historial"));
    }
}
