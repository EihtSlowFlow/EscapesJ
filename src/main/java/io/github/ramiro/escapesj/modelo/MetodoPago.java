package io.github.ramiro.escapesj.modelo;

/// Por si a futuro quiere aplicar descuento sobre otro medio de pago
public interface MetodoPago {
    void aplicarDescuento(float monto);
}
