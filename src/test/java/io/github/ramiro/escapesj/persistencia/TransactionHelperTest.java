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

import static org.junit.jupiter.api.Assertions.*;

public class TransactionHelperTest {

    private Connection conn;
    private BoletaRepository boletaRepo;
    private ProductoRepository productoRepo;
    private ServicioRepository servicioRepo;
    private FacturacionService facturacionService;

    public TransactionHelperTest() {
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Clean up test data
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM boleta_items WHERE codigo_producto = 'TEST-001' OR codigo_producto = 'TEST-002'");
            stmt.execute("DELETE FROM productos WHERE codigo = 'TEST-001' OR codigo = 'TEST-002'");
            stmt.execute("DELETE FROM servicios_historial WHERE nombre = 'Test Rollback Cliente' OR nombre = 'Test Commit Cliente'");
            stmt.execute("DELETE FROM boletas WHERE nombre_cliente = 'Test Rollback Cliente' OR nombre_cliente = 'Test Commit Cliente'");
        }
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
        boletaRepo = new BoletaRepository(conn);
        productoRepo = new ProductoRepository(conn);
        servicioRepo = new ServicioRepository(conn);
        facturacionService = new FacturacionService(boletaRepo, productoRepo, servicioRepo);

        productoRepo.guardar(new Producto("TEST-001", "Producto de Prueba", "Test", 1000.0, 10));
        productoRepo.guardar(new Producto("TEST-002", "Producto Sin Stock", "Test", 1000.0, 1)); // We will ask for 5

        int initialBoletas = countRows("SELECT COUNT(*) FROM boletas");
        int initialItems = countRows("SELECT COUNT(*) FROM boleta_items");
        int initialServicios = countRows("SELECT COUNT(*) FROM servicios_historial");

        FacturacionRequest request = new FacturacionRequest(
                "11111111", "Test Rollback Cliente", "2026-01-01",
                List.of(
                        new ItemFacturacion("PRODUCTO", "P1", "TEST-001", 2, 1000), // This one succeeds
                        new ItemFacturacion("PRODUCTO", "P2", "TEST-002", 5, 1000)  // This one will fail due to no stock
                ), 0
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
        boletaRepo = new BoletaRepository(conn);
        productoRepo = new ProductoRepository(conn);
        servicioRepo = new ServicioRepository(conn);
        facturacionService = new FacturacionService(boletaRepo, productoRepo, servicioRepo);

        productoRepo.guardar(new Producto("TEST-001", "Producto de Prueba", "Test", 1000.0, 10));

        int initialBoletas = countRows("SELECT COUNT(*) FROM boletas WHERE nombre_cliente = 'Test Commit Cliente'");
        int initialItems = countRows("SELECT COUNT(*) FROM boleta_items WHERE descripcion = 'P1'");
        int initialServicios = countRows("SELECT COUNT(*) FROM servicios_historial WHERE nombre = 'Test Commit Cliente'");

        FacturacionRequest request = new FacturacionRequest(
                "11111111", "Test Commit Cliente", "2026-01-01",
                List.of(
                        new ItemFacturacion("PRODUCTO", "P1", "TEST-001", 5, 1000)
                ), 0
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
        assertEquals(initialItems + 1, countRows("SELECT COUNT(*) FROM boleta_items WHERE descripcion = 'P1'"));
        assertEquals(initialServicios + 1, countRows("SELECT COUNT(*) FROM servicios_historial WHERE nombre = 'Test Commit Cliente'"));
    }
}
