package io.github.ramiro.escapesj.modelo;

public class PagoEfectivo implements MetodoPago {
    private float monto;

    public PagoEfectivo(float monto) {
        this.monto = monto;
    }

    @Override
    public void aplicarDescuento(float descuento) {
        this.monto -= descuento;
    }

}
