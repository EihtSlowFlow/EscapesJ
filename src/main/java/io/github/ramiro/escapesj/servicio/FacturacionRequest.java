package io.github.ramiro.escapesj.servicio;

import java.util.List;

public record FacturacionRequest(
        String dni,
        String nombreCliente,
        String fecha,
        List<ItemFacturacion> items,
        double descuentoPorcentaje
) {}
