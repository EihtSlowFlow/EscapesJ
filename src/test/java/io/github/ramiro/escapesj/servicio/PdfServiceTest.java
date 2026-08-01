package io.github.ramiro.escapesj.servicio;

import io.github.ramiro.escapesj.modelo.Emisor;
import io.github.ramiro.escapesj.persistencia.BoletaRepository.BoletaItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {

    @TempDir
    Path tempDir;

    private final Emisor emisor = new Emisor(
            1, "Escapes del Sur", "20123456789", "Mitre 123", "2920123456");

    @Test
    void generaBoletaConFechaLocalEnElNombre() {
        String ruta = BoletaPdfService.generarPdf(
                7, "2026-07-31", "12345678", "Cliente",
                List.of(new BoletaItem(1, "SERVICIO", "Soldadura", null, 1,
                        new BigDecimal("1500.00"), new BigDecimal("1500.00"))),
                new BigDecimal("1500.00"), "TRANSFERENCIA", BigDecimal.ZERO,
                tempDir.toString(), emisor);

        Path pdf = Path.of(ruta);
        assertEquals("Boleta_0007_31-07-2026.pdf", pdf.getFileName().toString());
        assertTrue(Files.isRegularFile(pdf));
        assertTrue(pdf.toFile().length() > 0);
    }

    @Test
    void generaPresupuestoConFechaLocalEnElNombre() {
        String ruta = PresupuestoPdfService.generarPdf(
                "P-ABC123", "2026-07-31", "2026-08-15", "12345678", "Cliente",
                List.of(new PresupuestoPdfService.ItemPresupuesto(
                        "Silenciador", 1, new BigDecimal("2000.00"), new BigDecimal("2000.00"))),
                new BigDecimal("2000.00"), tempDir.toString(), emisor);

        Path pdf = Path.of(ruta);
        assertEquals("Presupuesto_P-ABC123_31-07-2026.pdf", pdf.getFileName().toString());
        assertTrue(Files.isRegularFile(pdf));
        assertTrue(pdf.toFile().length() > 0);
    }
}
