package io.github.ramiro.escapesj.modelo; //

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class Cuit {
    private final String valor;

    private Cuit(String valor) {
        this.valor = valor;
    }

    public static Optional<Cuit> intentarCrear(String dni, String prefijo) {
        return Optional.ofNullable(dni)
                .filter(d -> d.matches("\\d+")) // Solo números
                .map(d -> calcular(d, prefijo))
                .map(Cuit::new);
    }

    private static String calcular(String dni, String prefijo) {
        String parcial = prefijo + String.format("%08d", Integer.parseInt(dni));
        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(parcial.charAt(i)) * pesos[i];
        }
        int resto = suma % 11;
        int verificador = (resto == 0) ? 0 : (resto == 1) ? 9 : 11 - resto;
        return parcial + verificador;
    }

    public <T> T transformar(Function<String, T> operacion) {
        return operacion.apply(this.valor);
    }

    public void usarComoIdentificador(Consumer<String> accion) {
        accion.accept(this.valor);
    }

    @Override
    public String toString() {
        return this.valor;
    }
}