package io.github.ramiro.escapesj.servicio;

import io.github.ramiro.escapesj.modelo.ServicioRealizado;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;
import io.github.ramiro.escapesj.persistencia.ServicioRepository;
import io.github.ramiro.escapesj.persistencia.TransactionHelper;

public class FacturacionService {

    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;

    public FacturacionService(BoletaRepository boletaRepository,
                              ProductoRepository productoRepository,
                              ServicioRepository servicioRepository) {
        this.boletaRepository = boletaRepository;
        this.productoRepository = productoRepository;
        this.servicioRepository = servicioRepository;
    }

    public FacturacionResult facturarOrden(FacturacionRequest request) throws Exception {
        double subtotal = request.items().stream()
                .mapToDouble(item -> item.precioUnitario() * item.cantidad())
                .sum();
        double descuentoMonto = subtotal * (request.descuentoPorcentaje() / 100.0);
        double totalFinal = subtotal - descuentoMonto;

        return TransactionHelper.runInTransaction(txConn -> {
            // 1. Crear boleta en DB
            int boletaId = boletaRepository.crearBoleta(txConn, request.dni(), request.nombreCliente(), request.fecha(), totalFinal);
            if (boletaId == -1) {
                throw new RuntimeException("No se pudo crear la boleta en la base de datos.");
            }

            // 2. Insertar ítems, descontar stock y registrar historial
            for (ItemFacturacion item : request.items()) {
                boletaRepository.agregarItem(txConn, boletaId, item.tipo(), item.descripcion(),
                        item.codigoProducto(), item.cantidad(), item.precioUnitario());

                if ("PRODUCTO".equals(item.tipo()) && item.codigoProducto() != null) {
                    boolean stockRestado = productoRepository.intentarRestarStock(txConn, item.codigoProducto(), item.cantidad());
                    if (!stockRestado) {
                        throw new RuntimeException("Stock insuficiente para el producto: " + item.descripcion());
                    }
                }

                servicioRepository.registrar(txConn, new ServicioRealizado(request.dni(), request.nombreCliente(),
                        item.tipo() + ": " + item.descripcion(), request.fecha()));
            }

            // 3. Obtener el número de boleta generado y los ítems guardados para el comprobante
            var itemsInsertados = boletaRepository.obtenerItems(txConn, boletaId);
            var boletas = boletaRepository.buscarBoletasPorDni(txConn, request.dni());
            int numeroBoleta = boletas.stream()
                    .filter(b -> b.id() == boletaId)
                    .map(b -> b.numero())
                    .findFirst()
                    .orElse(0);

            return new FacturacionResult(boletaId, numeroBoleta, subtotal, totalFinal, itemsInsertados);
        });
    }
}
