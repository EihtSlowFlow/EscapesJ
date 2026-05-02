package io.github.ramiro.escapesj.main;

import io.github.ramiro.escapesj.sdk.GeneradorOrdenCustom;

public class Principal {
    public static void main(String[] args) {
        new GeneradorOrdenCustom().generarOrdenFinal(
                "DB-9921", "Pérez, Ramiro", "Calle Falsa 123, Viedma", "20-12345678-9",
                "Contado (Efectivo)", "0001-00002506", "Silenciador Deportivo 3'", "1",
                16017.61, 10.0, "Efectivo"
        );
    }
}
