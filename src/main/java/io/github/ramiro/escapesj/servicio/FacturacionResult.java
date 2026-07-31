package io.github.ramiro.escapesj.servicio;

import io.github.ramiro.escapesj.persistencia.BoletaRepository.BoletaItem;
import java.math.BigDecimal;
import java.util.List;

public record FacturacionResult(
        int boletaId,
        int numero,
        BigDecimal subtotal,
        BigDecimal totalFinal,
        List<BoletaItem> items
) {}
