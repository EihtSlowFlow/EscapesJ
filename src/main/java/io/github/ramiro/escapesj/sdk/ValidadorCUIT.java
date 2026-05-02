package io.github.ramiro.escapesj.sdk;

public class ValidadorCUIT {

    public static boolean esMatematicamenteValido(String cuit) {
        if (cuit == null || cuit.length() != 11) return false;

        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;

        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(cuit.charAt(i)) * pesos[i];
        }

        int resto = suma % 11;
        int verificadorCalculado = (resto == 0) ? 0 : (resto == 1) ? 9 : 11 - resto;
        int verificadorReal = Character.getNumericValue(cuit.charAt(10));

        return verificadorCalculado == verificadorReal;
    }
}