package io.github.ramiro.escapesj.servicio;

import io.github.ramiro.escapesj.modelo.OperacionHistorica;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.persistencia.OperacionHistoricaRepository;
import io.github.ramiro.escapesj.persistencia.DatabaseService;
import io.github.ramiro.escapesj.persistencia.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

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
        assertNull(resumen.margenPorcentual());
        assertEquals(0, resumen.detalles().size());
    }

    @Test
    void testMargenDivisionPorCero() throws Exception {
        OperacionHistorica op1 = new OperacionHistorica(0, "2026-08-05", "REF-1", "Pepe", "", BigDecimal.ZERO, BigDecimal.ZERO, "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(op1);
        
        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);
        assertEquals(0, BigDecimal.ZERO.compareTo(resumen.facturacionConCostos()));
        assertNull(resumen.margenPorcentual());
        
        var detalle = resumen.detalles().get(0);
        assertNull(detalle.margen());
    }

    @Test
    void testGananciaNegativa() throws Exception {
        OperacionHistorica op1 = new OperacionHistorica(0, "2026-08-05", "REF-1", "Pepe", "", new BigDecimal("100.00"), new BigDecimal("150.00"), "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(op1);
        
        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);
        assertEquals(new BigDecimal("-50.00"), resumen.gananciaCalculable());
        assertEquals(new BigDecimal("-50.0000"), resumen.margenPorcentual());
    }
    
    @Test
    void testDescuentoConvierteEnPerdida() throws Exception {
        io.github.ramiro.escapesj.persistencia.ProductoRepository productoRepository = new io.github.ramiro.escapesj.persistencia.ProductoRepository();
        io.github.ramiro.escapesj.persistencia.ServicioRepository servicioRepository = new io.github.ramiro.escapesj.persistencia.ServicioRepository();
        io.github.ramiro.escapesj.servicio.FacturacionService facturacionService = new io.github.ramiro.escapesj.servicio.FacturacionService(boletaRepository, productoRepository, servicioRepository);
        
        productoRepository.guardar(new io.github.ramiro.escapesj.modelo.Producto("COD-DESC", "Prod Desc", "Desc", new BigDecimal("100.00"), 10, new BigDecimal("100.00")));
        
        ItemFacturacion item = new ItemFacturacion("PRODUCTO", "P1", "COD-DESC", 1, new BigDecimal("100.00"));
        FacturacionRequest req = new FacturacionRequest("111", "Pepe", "2026-08-01", java.util.List.of(item), "EFECTIVO", new BigDecimal("50.00"));
        
        facturacionService.facturarOrden(req);

        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);
        assertEquals(new BigDecimal("-50.00"), resumen.gananciaCalculable());
        assertEquals(new BigDecimal("-100.0000"), resumen.margenPorcentual());
    }

    @Test
    void testCambioDeCostoHistoricoNoAfectaBoleta() throws Exception {
        DatabaseService.getConnection().createStatement().execute("DELETE FROM productos");
        
        io.github.ramiro.escapesj.persistencia.ProductoRepository productoRepository = new io.github.ramiro.escapesj.persistencia.ProductoRepository();
        io.github.ramiro.escapesj.persistencia.ServicioRepository servicioRepository = new io.github.ramiro.escapesj.persistencia.ServicioRepository();
        io.github.ramiro.escapesj.servicio.FacturacionService facturacionService = new io.github.ramiro.escapesj.servicio.FacturacionService(boletaRepository, productoRepository, servicioRepository);
        
        io.github.ramiro.escapesj.modelo.Producto prod = new io.github.ramiro.escapesj.modelo.Producto("COD-1", "P1", "Desc", new BigDecimal("200.00"), 10, new BigDecimal("50.00"));
        productoRepository.guardar(prod);
        
        ItemFacturacion item = new ItemFacturacion("PRODUCTO", "P1", "COD-1", 1, new BigDecimal("200.00"));
        java.util.List<ItemFacturacion> items = java.util.List.of(item);
        FacturacionRequest req = new FacturacionRequest("222", "Juan", "2026-08-02", items, "EFECTIVO", BigDecimal.ZERO);
        facturacionService.facturarOrden(req);
        
        // Modificar el costo actual del producto
        io.github.ramiro.escapesj.modelo.Producto modificado = new io.github.ramiro.escapesj.modelo.Producto(prod.getCodigo(), prod.getNombre(), prod.getDescripcion(), prod.getPrecio(), prod.getStock(), new BigDecimal("20.00"));
        productoRepository.guardar(modificado);
        
        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);
        assertEquals(new BigDecimal("50.00"), resumen.costoConocido());
        assertEquals(new BigDecimal("150.00"), resumen.gananciaCalculable());
    }

    @Test
    void testBoletaIncompletaPorUnSoloItem() throws Exception {
        int b1Id = boletaRepository.crearBoleta("111", "Juan", "2026-08-10", new BigDecimal("1000.00"));
        boletaRepository.agregarItem(b1Id, "PRODUCTO", "P1", "COD-1", 1, new BigDecimal("500.00"), new BigDecimal("300.00"));
        boletaRepository.agregarItem(b1Id, "PRODUCTO", "P2", "COD-2", 1, new BigDecimal("500.00"), null);

        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);
        
        assertEquals(0, resumen.cantidadCompletas());
        assertEquals(1, resumen.cantidadIncompletas());
        assertEquals(new BigDecimal("1000.00"), resumen.facturacionSinCostos());
        
        var det = resumen.detalles().get(0);
        assertFalse(det.completa());
        assertNull(det.costo());
        assertNull(det.ganancia());
        assertNull(det.margen());
    }

    @Test
    void testDiciembreAEnero() throws Exception {
        OperacionHistorica opDic = new OperacionHistorica(0, "2026-12-15", "", "", "", new BigDecimal("100.00"), new BigDecimal("50.00"), "", "PENDIENTE", null, "", "");
        OperacionHistorica opEne = new OperacionHistorica(0, "2027-01-10", "", "", "", new BigDecimal("200.00"), new BigDecimal("100.00"), "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(opDic);
        operacionRepository.guardar(opEne);
        
        var resDic = rentabilidadService.calcularResumenMensual(2026, 12);
        assertEquals(new BigDecimal("100.00"), resDic.facturacionConCostos());
        
        var resEne = rentabilidadService.calcularResumenMensual(2027, 1);
        assertEquals(new BigDecimal("200.00"), resEne.facturacionConCostos());
    }

    @Test
    void testExclusionCorrectaDigitalYPapel() throws Exception {
        int b1Id = boletaRepository.crearBoleta("111", "Juan", "2026-08-10", new BigDecimal("1000.00"));
        boletaRepository.agregarItem(b1Id, "PRODUCTO", "P1", "COD-1", 1, new BigDecimal("1000.00"), new BigDecimal("500.00"));
        
        OperacionHistorica opDigitalizada = new OperacionHistorica(0, "2026-08-05", "REF", "Pepe", "", new BigDecimal("1000.00"), new BigDecimal("500.00"), "", "DIGITALIZADO", b1Id, "", "");
        operacionRepository.guardar(opDigitalizada);
        
        OperacionHistorica opPapel = new OperacionHistorica(0, "2026-08-15", "REF2", "Luis", "", new BigDecimal("500.00"), new BigDecimal("250.00"), "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(opPapel);
        
        var resumen = rentabilidadService.calcularResumenMensual(2026, 8);
        
        assertEquals(2, resumen.cantidadCompletas());
        assertEquals(new BigDecimal("1500.00"), resumen.facturacionConCostos());
        
        long countDigital = resumen.detalles().stream().filter(d -> "Digital".equals(d.origen())).count();
        long countPapel = resumen.detalles().stream().filter(d -> "Papel".equals(d.origen())).count();
        
        assertEquals(1, countDigital);
        assertEquals(1, countPapel);
    }
    
    @Test
    void testVinculacionDuplicadaRechazada() throws Exception {
        int b1Id = boletaRepository.crearBoleta("111", "Juan", "2026-08-10", new BigDecimal("1000.00"));
        
        OperacionHistorica op1 = new OperacionHistorica(0, "2026-08-05", "REF", "Pepe", "", new BigDecimal("1000.00"), new BigDecimal("500.00"), "", "DIGITALIZADO", b1Id, "", "");
        operacionRepository.guardar(op1);
        
        OperacionHistorica op2 = new OperacionHistorica(0, "2026-08-06", "REF2", "Luis", "", new BigDecimal("1000.00"), new BigDecimal("500.00"), "", "DIGITALIZADO", b1Id, "", "");
        
        assertThrows(PersistenceException.class, () -> operacionRepository.guardar(op2));
    }
    
    @Test
    void testMarcarDigitalizadoSinVincular() throws Exception {
        OperacionHistorica op1 = new OperacionHistorica(0, "2026-08-05", "REF", "Pepe", "", new BigDecimal("1000.00"), new BigDecimal("500.00"), "", "PENDIENTE", null, "", "");
        operacionRepository.guardar(op1);
        
        var guardada = operacionRepository.buscarTodas().stream().findFirst().get();
        assertEquals("PENDIENTE", guardada.getEstado());
        
        guardada.setEstado("DIGITALIZADO");
        operacionRepository.guardar(guardada);
        
        var actualizada = operacionRepository.buscarTodas().stream().findFirst().get();
        assertEquals("DIGITALIZADO", actualizada.getEstado());
        assertNull(actualizada.getBoletaDigitalId());
    }
}
