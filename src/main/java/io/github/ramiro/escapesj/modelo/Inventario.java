package io.github.ramiro.escapesj.modelo;

import io.github.ramiro.escapesj.persistencia.ProductoRepository;

import java.util.Optional;

public class Inventario {
    private final ProductoRepository repository;

    public Inventario(ProductoRepository repository) {
        this.repository = repository;
    }

    public Optional<Producto> buscarPorCodigo(String codigo) {
        return repository.buscarPorCodigo(codigo);
    }

    public boolean procesarVenta(String codigo, int cantidad) {
        return repository.verificarStockDisponible(codigo, cantidad);
    }

    public boolean verificarStockDisponible(String codigo, int cantidad) {
        return repository.verificarStockDisponible(codigo, cantidad);
    }

    public void restaurarStock(String codigo, int cantidad) {
        // Obsoleto si no se resta prematuramente, pero lo dejamos vacío
        // o llamamos a sumarStock si hiciera falta.
        // repository.sumarStock(codigo, cantidad);
    }
}