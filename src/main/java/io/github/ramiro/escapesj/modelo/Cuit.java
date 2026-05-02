package io.github.ramiro.escapesj.modelo;

public class Cuit {
    private final String valor;

    public Cuit(String dni, String prefijo) {
        this.valor = construirYValidar(dni, prefijo);
    }

    private String construirYValidar(String dni, String prefijo) {
        String parcial = prefijo + String.format("%08d", Integer.parseInt(dni));
        int verificador = calcularModulo11(parcial);
        return parcial + verificador;
    }

    private int calcularModulo11(String parcial) {
        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(parcial.charAt(i)) * pesos[i];
        }
        int resto = suma % 11;
        return (resto == 0) ? 0 : (resto == 1) ? 9 : 11 - resto;
    }

    public void usarComoIdentificador(java.util.function.Consumer<String> accion) {
        accion.accept(this.valor);
    }

    @Override
    public String toString() {
        return this.valor;
    }
}