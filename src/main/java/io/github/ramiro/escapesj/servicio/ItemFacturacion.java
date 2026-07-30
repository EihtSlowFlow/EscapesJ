package io.github.ramiro.escapesj.servicio;

public record ItemFacturacion(
        String tipo,
        String descripcion,
        String codigoProducto,
        int cantidad,
        double precioUnitario
) {}
