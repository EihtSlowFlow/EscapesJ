package io.github.ramiro.escapesj.modelo;

public class Cliente {
    // incremental
    int numeroCliente;
    String nombre;
    String apellido;
    

    public String calcularCUIL(String dni, int prefijo) {
        // Aseguramos que el DNI tenga 8 dígitos (completando con ceros a la izquierda si hace falta)
        String dniFull = String.format("%08d", Integer.parseInt(dni));
        String base = prefijo + dniFull;

        int[] multiplicadores = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;

        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(base.charAt(i)) * multiplicadores[i];
        }

        int resto = suma % 11;
        int verificador;

        if (resto == 0) {
            verificador = 0;
        } else if (resto == 1) {
            // Caso especial: Si el resto es 1, el prefijo cambia a 23 
            // y el verificador suele ser 9 (hombres) o 4 (mujeres).
            // Para simplificar tu búsqueda, si falla con 20/27, podrías probar el caso 23.
            return calcularCUIL(dni, 23);
        } else {
            verificador = 11 - resto;
        }

        return base + verificador;
    }

}
