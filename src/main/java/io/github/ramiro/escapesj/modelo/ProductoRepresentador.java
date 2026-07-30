package io.github.ramiro.escapesj.modelo;

import java.math.BigDecimal;

public interface ProductoRepresentador {
    void definirCodigo(String codigo);

    void definirDescripcion(String descripcion);

    void definirPrecio(BigDecimal precio);
}