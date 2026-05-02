package io.github.ramiro.escapesj.modelo;

import java.util.function.Consumer;

public class Producto {
    private final String codigo;
    private final String descripcion;
    private final double precioBase;

    public Producto(String codigo, String descripcion, double precioBase) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precioBase = precioBase;
    }

    public boolean coincideCodigo(String busqueda) {
        return this.codigo.equalsIgnoreCase(busqueda);
    }

    public void presentarseEn(ProductoRepresentador representador) {
        representador.definirCodigo(this.codigo);
        representador.definirDescripcion(this.descripcion);
        representador.definirPrecio(this.precioBase);
    }

    public void representarEnFila(Consumer<Object[]> receptor) {
        receptor.accept(new Object[]{this.codigo, this.descripcion, this.precioBase});
    }

    @Override
    public String toString() {
        return String.format("%s - %s ($%.2f)", codigo, descripcion, precioBase);
    }
}