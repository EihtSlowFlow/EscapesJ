package io.github.ramiro.escapesj.servicio;

import java.math.BigDecimal;
import java.util.List;

public record FacturacionRequest(
        String dni,
        String nombreCliente,
        String fecha,
        List<ItemFacturacion> items,
        String metodoPago,
        BigDecimal descuentoPorcentaje
) {}
