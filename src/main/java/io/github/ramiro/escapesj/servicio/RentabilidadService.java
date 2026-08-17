package io.github.ramiro.escapesj.servicio;

import io.github.ramiro.escapesj.modelo.OperacionHistorica;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.persistencia.OperacionHistoricaRepository;
import io.github.ramiro.escapesj.persistencia.TransactionHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RentabilidadService {
    private final BoletaRepository boletaRepository;
    private final OperacionHistoricaRepository operacionHistoricaRepository;

    public RentabilidadService(BoletaRepository boletaRepository, OperacionHistoricaRepository operacionHistoricaRepository) {
        this.boletaRepository = boletaRepository;
        this.operacionHistoricaRepository = operacionHistoricaRepository;
    }

    public ResumenRentabilidad calcularResumenMensual(int anio, int mes) throws Exception {
        LocalDate inicioMes = LocalDate.of(anio, mes, 1);
        LocalDate finMes = inicioMes.plusMonths(1);
        
        String fechaInicioStr = inicioMes.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String fechaFinStr = finMes.format(DateTimeFormatter.ISO_LOCAL_DATE);

        return TransactionHelper.runInTransaction(txConn -> {
            List<BoletaRepository.BoletaResumen> boletas = boletaRepository.obtenerBoletasPorRango(txConn, fechaInicioStr, fechaFinStr);
            List<OperacionHistorica> operacionesPapel = operacionHistoricaRepository.buscarPorRangoFechaYEstado(txConn, fechaInicioStr, fechaFinStr, "PENDIENTE");

            BigDecimal facturacionConCostos = BigDecimal.ZERO;
            BigDecimal costoConocido = BigDecimal.ZERO;
            BigDecimal facturacionSinCostos = BigDecimal.ZERO;
            int cantidadIncompletas = 0;
            int cantidadCompletas = 0;

            // Procesar Boletas Digitales
            for (BoletaRepository.BoletaResumen boleta : boletas) {
                List<BoletaRepository.BoletaItem> items = boletaRepository.obtenerItems(txConn, boleta.id());
                
                boolean boletaCompleta = true;
                BigDecimal costoTotalBoleta = BigDecimal.ZERO;

                for (BoletaRepository.BoletaItem item : items) {
                    if (item.costoUnitarioHistorico() == null) {
                        boletaCompleta = false;
                        break;
                    }
                    costoTotalBoleta = costoTotalBoleta.add(item.costoUnitarioHistorico().multiply(BigDecimal.valueOf(item.cantidad())));
                }

                if (boletaCompleta) {
                    facturacionConCostos = facturacionConCostos.add(boleta.total());
                    costoConocido = costoConocido.add(costoTotalBoleta);
                    cantidadCompletas++;
                } else {
                    facturacionSinCostos = facturacionSinCostos.add(boleta.total());
                    cantidadIncompletas++;
                }
            }

            // Procesar Operaciones en Papel (solo las PENDIENTES, ya traídas por query)
            for (OperacionHistorica op : operacionesPapel) {
                if (op.getCostoMateriales() == null) {
                    facturacionSinCostos = facturacionSinCostos.add(op.getImporteTotal());
                    cantidadIncompletas++;
                } else {
                    facturacionConCostos = facturacionConCostos.add(op.getImporteTotal());
                    costoConocido = costoConocido.add(op.getCostoMateriales());
                    cantidadCompletas++;
                }
            }

            BigDecimal gananciaCalculable = facturacionConCostos.subtract(costoConocido);

            return new ResumenRentabilidad(
                    facturacionConCostos,
                    costoConocido,
                    gananciaCalculable,
                    facturacionSinCostos,
                    cantidadIncompletas,
                    cantidadCompletas
            );
        });
    }

    public record ResumenRentabilidad(
            BigDecimal facturacionConCostos,
            BigDecimal costoConocido,
            BigDecimal gananciaCalculable,
            BigDecimal facturacionSinCostos,
            int cantidadIncompletas,
            int cantidadCompletas
    ) {
        public BigDecimal getFacturacionTotal() {
            return facturacionConCostos.add(facturacionSinCostos);
        }

        public boolean tieneResultadosParciales() {
            return cantidadIncompletas > 0;
        }
    }
}
