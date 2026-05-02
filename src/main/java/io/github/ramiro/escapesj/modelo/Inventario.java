package io.github.ramiro.escapesj.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Inventario {
    private final List<Producto> productos;

    public Inventario() {
        this.productos = new ArrayList<>();
        // TODO: Aquí cargarás los datos desde la DB en el futuro
    }

    public Optional<Producto> buscarPorCodigo(String codigo) {
        return productos.stream()
                .filter(p -> p.coincideCodigo(codigo))
                .findFirst();
    }

    public void agregarProducto(Producto nuevo) {
        Optional.ofNullable(nuevo).ifPresent(productos::add);
    }
}