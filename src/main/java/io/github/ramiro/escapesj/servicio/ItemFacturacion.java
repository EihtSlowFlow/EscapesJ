package io.github.ramiro.escapesj.servicio;

import java.math.BigDecimal;

public record ItemFacturacion(
        String tipo,
        String descripcion,
        String codigoProducto,
        int cantidad,
        BigDecimal precioUnitario
) {}
