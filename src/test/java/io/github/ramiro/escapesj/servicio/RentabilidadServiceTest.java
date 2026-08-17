package io.github.ramiro.escapesj.servicio;

import io.github.ramiro.escapesj.modelo.OperacionHistorica;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.persistencia.OperacionHistoricaRepository;
import io.github.ramiro.escapesj.persistencia.DatabaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class RentabilidadServiceTest {

    @TempDir
    Path tempDir;

    private RentabilidadService rentabilidadService;
    private BoletaRepository boletaRepository;
    private OperacionHistoricaRepository operacionRepository;

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-rentabilidad.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + dbPath.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();

        boletaRepository = new BoletaRepository();
        operacionRepository = new OperacionHistoricaRepository();
        rentabilidadService = new RentabilidadService(boletaRepository, operacionRepository);
    }

    @AfterEach
    void tearDown() {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    void testCalculoVacio() throws Exception {
        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);
        assertEquals(BigDecimal.ZERO, resumen.facturacionConCostos());
        assertEquals(BigDecimal.ZERO, resumen.costoConocido());
        assertEquals(BigDecimal.ZERO, resumen.gananciaCalculable());
        assertEquals(BigDecimal.ZERO, resumen.facturacionSinCostos());
        assertEquals(0, resumen.cantidadIncompletas());
        assertEquals(0, resumen.cantidadCompletas());
        assertFalse(resumen.tieneResultadosParciales());
    }

    @Test
    void testBoletaCompletaYBoletaIncompleta() throws Exception {
        // Boleta completa
        int b1Id = boletaRepository.crearBoleta("111", "Juan", "2026-08-10", new BigDecimal("1000.00"));
        boletaRepository.agregarItem(b1Id, "PRODUCTO", "P1", "COD-1", 2, new BigDecimal("500.00"), new BigDecimal("300.00"));

        // Boleta incompleta (costo null)
        int b2Id = boletaRepository.crearBoleta("222", "Ana", "2026-08-15", new BigDecimal("2000.00"));
        boletaRepository.agregarItem(b2Id, "PRODUCTO", "P2", "COD-2", 1, new BigDecimal("2000.00"), null);

        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);
        
        assertEquals(new BigDecimal("1000.00"), resumen.facturacionConCostos()); // Solo b1 suma
        assertEquals(new BigDecimal("600.00"), resumen.costoConocido()); // 2 * 300
        assertEquals(new BigDecimal("400.00"), resumen.gananciaCalculable()); // 1000 - 600

        assertEquals(new BigDecimal("2000.00"), resumen.facturacionSinCostos()); // Solo b2 suma acá
        assertEquals(1, resumen.cantidadCompletas());
        assertEquals(1, resumen.cantidadIncompletas());
        assertTrue(resumen.tieneResultadosParciales());
        assertEquals(new BigDecimal("3000.00"), resumen.getFacturacionTotal());
    }

    @Test
    void testOperacionPapelCompletaEIncompleta() throws Exception {
        OperacionHistorica op1 = new OperacionHistorica(
                0, "2026-08-05", "REF-1", "Pepe", "Cambio aceite",
                new BigDecimal("5000.00"), new BigDecimal("2000.00"), "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(op1);

        OperacionHistorica op2 = new OperacionHistorica(
                0, "2026-08-20", "REF-2", "Luis", "Revisión",
                new BigDecimal("3000.00"), null, "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(op2);

        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);

        assertEquals(new BigDecimal("5000.00"), resumen.facturacionConCostos());
        assertEquals(new BigDecimal("2000.00"), resumen.costoConocido());
        assertEquals(new BigDecimal("3000.00"), resumen.gananciaCalculable());

        assertEquals(new BigDecimal("3000.00"), resumen.facturacionSinCostos());
        assertEquals(1, resumen.cantidadCompletas());
        assertEquals(1, resumen.cantidadIncompletas());
        assertTrue(resumen.tieneResultadosParciales());
    }

    @Test
    void testFiltradoPorMes() throws Exception {
        OperacionHistorica op1 = new OperacionHistorica(
                0, "2026-07-31", "REF-1", "Pepe", "A",
                new BigDecimal("100.00"), new BigDecimal("50.00"), "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(op1);

        OperacionHistorica op2 = new OperacionHistorica(
                0, "2026-08-01", "REF-2", "Luis", "B",
                new BigDecimal("200.00"), new BigDecimal("100.00"), "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(op2);

        OperacionHistorica op3 = new OperacionHistorica(
                0, "2026-09-01", "REF-3", "Luis", "C",
                new BigDecimal("400.00"), new BigDecimal("200.00"), "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(op3);

        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);

        // Solo la de agosto debe sumar
        assertEquals(new BigDecimal("200.00"), resumen.facturacionConCostos());
        assertEquals(new BigDecimal("100.00"), resumen.costoConocido());
        assertEquals(1, resumen.cantidadCompletas());
    }

    @Test
    void testOperacionPapelDigitalizadaNoSuma() throws Exception {
        OperacionHistorica op1 = new OperacionHistorica(
                0, "2026-08-05", "REF-1", "Pepe", "Cambio aceite",
                new BigDecimal("5000.00"), new BigDecimal("2000.00"), "", "DIGITALIZADO", null, "", "");
        operacionRepository.guardar(op1);

        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);

        assertEquals(0, resumen.cantidadCompletas());
        assertEquals(BigDecimal.ZERO, resumen.facturacionConCostos());
    }
}
