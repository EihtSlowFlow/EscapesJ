package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.Producto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionHelperTest {

    private Connection conn;
    private BoletaRepository boletaRepo;
    private ProductoRepository productoRepo;

    public TransactionHelperTest() {
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Clean up test data
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM boleta_items WHERE codigo_producto = 'TEST-001'");
            stmt.execute("DELETE FROM productos WHERE codigo = 'TEST-001'");
            stmt.execute("DELETE FROM boletas WHERE nombre_cliente = 'Test Rollback Cliente'");
        }
    }

    @Test
    public void testTransactionRollbacksOnError() throws Exception {
        conn = DatabaseService.getConnection();
        boletaRepo = new BoletaRepository(conn);
        productoRepo = new ProductoRepository(conn);
        productoRepo.guardar(new Producto("TEST-001", "Producto de Prueba", "Test", 1000.0, 10));

        // We will attempt to create a boleta and subtract stock, then force an error
        Exception exception = assertThrows(RuntimeException.class, () -> {
            TransactionHelper.runInTransaction(txConn -> {
                // 1. Create boleta
                int boletaId = boletaRepo.crearBoleta(txConn, "11111111", "Test Rollback Cliente", "2026-01-01", 1000.0);
                assertTrue(boletaId > 0, "Boleta should be created successfully in memory");

                // 2. Subtract stock
                boolean stockRestado = productoRepo.intentarRestarStock(txConn, "TEST-001", 5);
                assertTrue(stockRestado, "Stock should be subtracted successfully in memory");

                // 3. Force an error to trigger rollback
                throw new RuntimeException("Forced error to test rollback");
            });
        });

        assertEquals("Forced error to test rollback", exception.getMessage());

        // Verify that the rollback was successful
        
        // 1. Check stock is back to 10
        var productoOpt = productoRepo.buscarPorCodigo("TEST-001");
        assertTrue(productoOpt.isPresent());
        assertEquals(10, productoOpt.get().getStock(), "Stock should be rolled back to 10");

        // 2. Check that boleta doesn't exist
        var boletas = boletaRepo.buscarBoletasPorDni("11111111");
        boolean boletaExists = boletas.stream().anyMatch(b -> b.nombreCliente().equals("Test Rollback Cliente"));
        assertFalse(boletaExists, "Boleta should not exist because of rollback");
    }

    @Test
    public void testTransactionCommitsOnSuccess() throws Exception {
        conn = DatabaseService.getConnection();
        boletaRepo = new BoletaRepository(conn);
        productoRepo = new ProductoRepository(conn);
        productoRepo.guardar(new Producto("TEST-001", "Producto de Prueba", "Test", 1000.0, 10));

        TransactionHelper.runInTransaction(txConn -> {
            // 1. Create boleta
            int boletaId = boletaRepo.crearBoleta(txConn, "11111111", "Test Commit Cliente", "2026-01-01", 1000.0);
            
            // 2. Subtract stock
            productoRepo.intentarRestarStock(txConn, "TEST-001", 5);
            
            return boletaId;
        });

        // Verify that the commit was successful
        
        // 1. Check stock is now 5
        var productoOpt = productoRepo.buscarPorCodigo("TEST-001");
        assertTrue(productoOpt.isPresent());
        assertEquals(5, productoOpt.get().getStock(), "Stock should be committed to 5");

        // Cleanup
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM boletas WHERE nombre_cliente = 'Test Commit Cliente'");
        }
    }
}
