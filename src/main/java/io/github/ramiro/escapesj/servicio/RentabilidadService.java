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

            java.util.List<DetalleRentabilidad> detalles = new java.util.ArrayList<>();

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
                    
                    BigDecimal ganancia = boleta.total().subtract(costoTotalBoleta);
                    BigDecimal margen = (boleta.total().compareTo(BigDecimal.ZERO) > 0)
                            ? ganancia.divide(boleta.total(), 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : null;
                    detalles.add(new DetalleRentabilidad(boleta.id(), "Digital", boleta.fecha(), boleta.nombreCliente(), boleta.total(), costoTotalBoleta, ganancia, margen, true));
                } else {
                    facturacionSinCostos = facturacionSinCostos.add(boleta.total());
                    cantidadIncompletas++;
                    detalles.add(new DetalleRentabilidad(boleta.id(), "Digital", boleta.fecha(), boleta.nombreCliente(), boleta.total(), null, null, null, false));
                }
            }

            // Procesar Operaciones en Papel (solo las PENDIENTES, ya traídas por query)
            for (OperacionHistorica op : operacionesPapel) {
                if (op.getCostoMateriales() == null) {
                    facturacionSinCostos = facturacionSinCostos.add(op.getImporteTotal());
                    cantidadIncompletas++;
                    detalles.add(new DetalleRentabilidad(op.getId(), "Papel", op.getFecha(), op.getCliente(), op.getImporteTotal(), null, null, null, false));
                } else {
                    facturacionConCostos = facturacionConCostos.add(op.getImporteTotal());
                    costoConocido = costoConocido.add(op.getCostoMateriales());
                    cantidadCompletas++;
                    
                    BigDecimal ganancia = op.getImporteTotal().subtract(op.getCostoMateriales());
                    BigDecimal margen = (op.getImporteTotal().compareTo(BigDecimal.ZERO) > 0)
                            ? ganancia.divide(op.getImporteTotal(), 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : null;
                    detalles.add(new DetalleRentabilidad(op.getId(), "Papel", op.getFecha(), op.getCliente(), op.getImporteTotal(), op.getCostoMateriales(), ganancia, margen, true));
                }
            }
            
            detalles.sort((a, b) -> b.fecha().compareTo(a.fecha()));

            BigDecimal gananciaCalculable = facturacionConCostos.subtract(costoConocido);
            BigDecimal margenPorcentual = (facturacionConCostos.compareTo(BigDecimal.ZERO) > 0)
                    ? gananciaCalculable.divide(facturacionConCostos, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : null;

            return new ResumenRentabilidad(
                    facturacionConCostos,
                    costoConocido,
                    gananciaCalculable,
                    margenPorcentual,
                    facturacionSinCostos,
                    cantidadIncompletas,
                    cantidadCompletas,
                    detalles
            );
        });
    }
    
    public record DetalleRentabilidad(
        Integer id,
        String origen,
        String fecha,
        String cliente,
        BigDecimal facturacion,
        BigDecimal costo,
        BigDecimal ganancia,
        BigDecimal margen,
        boolean completa
    ) {}

    public record ResumenRentabilidad(
            BigDecimal facturacionConCostos,
            BigDecimal costoConocido,
            BigDecimal gananciaCalculable,
            BigDecimal margenPorcentual,
            BigDecimal facturacionSinCostos,
            int cantidadIncompletas,
            int cantidadCompletas,
            List<DetalleRentabilidad> detalles
    ) {
        public BigDecimal getFacturacionTotal() {
            return facturacionConCostos.add(facturacionSinCostos);
        }

        public boolean tieneResultadosParciales() {
            return cantidadIncompletas > 0;
        }
    }

    public enum FiltroOrigen {
        TODAS("Todas"),
        DIGITALES("Digitales"),
        PAPEL("Papel");

        private final String descripcion;

        FiltroOrigen(String descripcion) {
            this.descripcion = descripcion;
        }

        @Override
        public String toString() {
            return descripcion;
        }
    }

    public ResumenRentabilidad filtrarPorOrigen(ResumenRentabilidad resumenCompleto, FiltroOrigen origen) {
        if (origen == FiltroOrigen.TODAS) {
            return resumenCompleto;
        }

        List<DetalleRentabilidad> filtrados = resumenCompleto.detalles().stream()
                .filter(d -> (origen == FiltroOrigen.DIGITALES && "Digital".equals(d.origen())) ||
                             (origen == FiltroOrigen.PAPEL && "Papel".equals(d.origen())))
                .collect(java.util.stream.Collectors.toList());

        BigDecimal facturacionConCostos = BigDecimal.ZERO;
        BigDecimal costoConocido = BigDecimal.ZERO;
        BigDecimal facturacionSinCostos = BigDecimal.ZERO;
        int cantidadIncompletas = 0;
        int cantidadCompletas = 0;

        for (DetalleRentabilidad d : filtrados) {
            if (d.completa()) {
                facturacionConCostos = facturacionConCostos.add(d.facturacion());
                costoConocido = costoConocido.add(d.costo());
                cantidadCompletas++;
            } else {
                facturacionSinCostos = facturacionSinCostos.add(d.facturacion());
                cantidadIncompletas++;
            }
        }

        BigDecimal gananciaCalculable = facturacionConCostos.subtract(costoConocido);
        BigDecimal margenPorcentual = (facturacionConCostos.compareTo(BigDecimal.ZERO) > 0)
                ? gananciaCalculable.divide(facturacionConCostos, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : null;

        return new ResumenRentabilidad(
                facturacionConCostos,
                costoConocido,
                gananciaCalculable,
                margenPorcentual,
                facturacionSinCostos,
                cantidadIncompletas,
                cantidadCompletas,
                filtrados
        );
    }
}
