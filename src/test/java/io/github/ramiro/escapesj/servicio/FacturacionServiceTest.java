package io.github.ramiro.escapesj.servicio;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.persistencia.DatabaseService;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.persistencia.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FacturacionServiceTest {

    private FacturacionService facturacionService;

    @BeforeEach
    public void setup() throws Exception {
        DatabaseService.inicializar(); // ensures DB is seeded/migrated
        BoletaRepository boletaRepo = new BoletaRepository();
        ProductoRepository productoRepo = new ProductoRepository();
        ServicioRepository servicioRepo = new ServicioRepository();
        facturacionService = new FacturacionService(boletaRepo, productoRepo, servicioRepo);
        
        productoRepo.guardar(new Producto("TEST-F-1", "Test", "Test", new BigDecimal("1000.00"), 100));
        productoRepo.guardar(new Producto("TEST-F-2", "Test2", "Test", new BigDecimal("0.10"), 100));
        productoRepo.guardar(new Producto("TEST-F-3", "Test3", "Test", new BigDecimal("0.20"), 100));
    }

    @Test
    public void testSumaBinariaNoFallaConBigDecimal() throws Exception {
        FacturacionRequest request = new FacturacionRequest(
                "12345678", "Test Cliente", "2026-01-01",
                List.of(
                        new ItemFacturacion("PRODUCTO", "Test", "TEST-F-2", 1, new BigDecimal("0.10")),
                        new ItemFacturacion("PRODUCTO", "Test2", "TEST-F-3", 1, new BigDecimal("0.20"))
                ), "TRANSFERENCIA", BigDecimal.ZERO
        );

        FacturacionResult result = facturacionService.facturarOrden(request);

        assertEquals(new BigDecimal("0.30"), result.subtotal());
        assertEquals(new BigDecimal("0.30"), result.totalFinal());
    }

    @Test
    public void testDescuentoRedondeoMedioCentavo() throws Exception {
        FacturacionRequest request = new FacturacionRequest(
                "12345678", "Test Cliente", "2026-01-01",
                List.of(
                        new ItemFacturacion("PRODUCTO", "Test", "TEST-F-1", 1, new BigDecimal("100.00"))
                ), "EFECTIVO", new BigDecimal("12.345")
        );

        FacturacionResult result = facturacionService.facturarOrden(request);

        assertEquals(0, new BigDecimal("100.00").compareTo(result.subtotal()));
        assertEquals(0, new BigDecimal("87.65").compareTo(result.totalFinal())); // 12.35 discount
    }

    @Test
    public void testRechazoValoresNegativos() {
        assertThrows(IllegalArgumentException.class, () -> {
            io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(new BigDecimal("-10.00"));
        });
    }

    @Test
    public void testRechazoPrecisionMayorADos() {
        assertThrows(ArithmeticException.class, () -> {
            io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(new BigDecimal("1.005"));
        });
    }

    @Test
    public void testAceptaValoresExactos() {
        assertEquals(100L, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(new BigDecimal("1")));
        assertEquals(100L, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(new BigDecimal("1.0")));
        assertEquals(100L, io.github.ramiro.escapesj.sdk.DineroUtil.aCentavos(new BigDecimal("1.00")));
    }

    @Test
    public void testDescuentoCero() throws Exception {
        FacturacionRequest request = new FacturacionRequest(
                "12345678", "Test", "2026-01-01",
                List.of(new ItemFacturacion("PRODUCTO", "Test", "TEST-F-1", 1, new BigDecimal("100.00"))),
                "EFECTIVO", BigDecimal.ZERO
        );
        FacturacionResult result = facturacionService.facturarOrden(request);
        assertEquals(0, new BigDecimal("100.00").compareTo(result.totalFinal()));
    }
}
