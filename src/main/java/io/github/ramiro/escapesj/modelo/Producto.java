package io.github.ramiro.escapesj.modelo;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class Producto {
    private String codigo, nombre, descripcion;
    private BigDecimal precio;
    private BigDecimal costoUnitario;
    private int stock;

    public Producto(String codigo, String nombre, String descripcion, BigDecimal precio, int stock) {
        this(codigo, nombre, descripcion, precio, stock, null);
    }

    public Producto(String codigo, String nombre, String descripcion, BigDecimal precio, int stock, BigDecimal costoUnitario) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.costoUnitario = costoUnitario;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public int getStock() {
        return stock;
    }

    public void representarEnFila(Consumer<Object[]> receptor) {
        receptor.accept(new Object[]{codigo, nombre, precio, stock});
    }

    public void presentarseEn(ProductoRepresentador r) {
        r.definirCodigo(codigo);
        r.definirDescripcion(nombre); // Usamos nombre como descripción principal en la venta
        r.definirPrecio(precio);
    }
}