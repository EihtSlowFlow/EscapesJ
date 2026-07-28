package io.github.ramiro.escapesj.modelo;

public class ServicioRealizado {
    private String dni, nombre, trabajo, fecha;

    public ServicioRealizado(String dni, String nombre, String trabajo, String fecha) {
        this.dni = dni;
        this.nombre = nombre;
        this.trabajo = trabajo;
        this.fecha = fecha;
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTrabajo() {
        return trabajo;
    }

    public String getFecha() {
        return fecha;
    }
}