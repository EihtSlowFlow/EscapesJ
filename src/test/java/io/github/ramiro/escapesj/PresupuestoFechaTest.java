package io.github.ramiro.escapesj;

import io.github.ramiro.escapesj.vista.VentanaPresupuesto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PresupuestoFechaTest {

    @Test
    @DisplayName("Una fecha anterior al día de hoy NO debe ser válida para crear un presupuesto")
    public void testFechaAnteriorAHoyEsInvalida() {
        LocalDate hoy = LocalDate.of(2026, 7, 28);
        LocalDate fechaAyer = hoy.minusDays(1);
        LocalDate fechaMesPasado = hoy.minusMonths(1);

        assertFalse(VentanaPresupuesto.esFechaLimiteValida(fechaAyer, hoy),
                "Una fecha anterior al día de hoy (ayer) no debe ser aceptada como fecha límite.");
        assertFalse(VentanaPresupuesto.esFechaLimiteValida(fechaMesPasado, hoy),
                "Una fecha de un mes atrás no debe ser aceptada como fecha límite.");
    }

    @Test
    @DisplayName("El día de hoy tampoco debe ser válido como fecha límite futura (debe ser posterior a hoy)")
    public void testFechaHoyEsInvalida() {
        LocalDate hoy = LocalDate.of(2026, 7, 28);

        assertFalse(VentanaPresupuesto.esFechaLimiteValida(hoy, hoy),
                "El día de hoy no es posterior a hoy; el presupuesto debe expirar en el futuro.");
    }

    @Test
    @DisplayName("Una fecha posterior a hoy SÍ debe ser válida para la creación del presupuesto")
    public void testFechaFuturaEsValida() {
        LocalDate hoy = LocalDate.of(2026, 7, 28);
        LocalDate manana = hoy.plusDays(1);
        LocalDate enTreintaDias = hoy.plusDays(30);

        assertTrue(VentanaPresupuesto.esFechaLimiteValida(manana, hoy),
                "Mañana debe ser una fecha límite válida.");
        assertTrue(VentanaPresupuesto.esFechaLimiteValida(enTreintaDias, hoy),
                "Una fecha a 30 días debe ser una fecha límite válida.");
    }

    @Test
    @DisplayName("Verificación de estado de presupuesto vencido cuando la fecha límite es anterior a hoy")
    public void testPresupuestoVencidoConFechaAnterior() {
        LocalDate hoy = LocalDate.of(2026, 7, 28);
        String fechaLimiteISOAnterior = "2026-07-20";

        assertFalse(VentanaPresupuesto.esPresupuestoVigente(fechaLimiteISOAnterior, hoy),
                "Un presupuesto con fecha límite anterior a hoy debe ser considerado VENCIDO.");
    }

    @Test
    @DisplayName("Verificación de estado de presupuesto vigente cuando la fecha límite es hoy o futura")
    public void testPresupuestoVigenteConFechaFutura() {
        LocalDate hoy = LocalDate.of(2026, 7, 28);
        String fechaLimiteISOHoy = "2026-07-28";
        String fechaLimiteISOFutura = "2026-08-28";

        assertTrue(VentanaPresupuesto.esPresupuestoVigente(fechaLimiteISOHoy, hoy),
                "Un presupuesto cuya fecha límite es el día de hoy sigue vigente hasta que termine el día.");
        assertTrue(VentanaPresupuesto.esPresupuestoVigente(fechaLimiteISOFutura, hoy),
                "Un presupuesto con fecha límite futura debe estar VIGENTE.");
    }
}
