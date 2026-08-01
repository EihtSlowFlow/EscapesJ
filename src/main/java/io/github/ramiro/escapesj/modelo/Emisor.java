package io.github.ramiro.escapesj.modelo;

public record Emisor(
    int id,
    String nombre,
    String cuit,
    String calle,
    String telefono
) {
    @Override
    public String toString() {
        return nombre + " (CUIT: " + cuit + ")";
    }
}
