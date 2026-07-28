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
        return repository.intentarRestarStock(codigo, cantidad);
    }

    public void restaurarStock(String codigo, int cantidad) {
        repository.sumarStock(codigo, cantidad);
    }
}