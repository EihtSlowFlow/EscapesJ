package io.github.ramiro.escapesj.modelo;

public class Cliente {
    private final String nombre;
    private final Cuit cuit;

    public Cliente(String nombre, Cuit cuit) {
        this.nombre = nombre;
        this.cuit = cuit;
    }

    public void representarEn(java.util.Map<String, Object> documento) {
        documento.put("nombre_cliente", this.nombre);
        documento.put("cuit_cliente", this.cuit.toString());
    }

    public void presentarseEn(ClienteRepresentador representador) {
        this.cuit.usarComoIdentificador(representador::definirDni);
        representador.definirNombre(this.nombre);
    }

    // Solo para pruebas
    public String getNombre() { return this.nombre; }
    public Cuit getCuit() { return this.cuit; }
}