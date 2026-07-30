package io.github.ramiro.escapesj.servicio;

import io.github.ramiro.escapesj.persistencia.BoletaRepository.BoletaItem;
import java.util.List;

public record FacturacionResult(
        int boletaId,
        int numero,
        double subtotal,
        double totalFinal,
        List<BoletaItem> items
) {}
