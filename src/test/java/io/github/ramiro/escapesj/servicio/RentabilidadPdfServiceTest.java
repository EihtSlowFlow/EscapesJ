package io.github.ramiro.escapesj.servicio;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.github.ramiro.escapesj.servicio.RentabilidadService.ResumenRentabilidad;
import io.github.ramiro.escapesj.servicio.RentabilidadService.DetalleRentabilidad;
import io.github.ramiro.escapesj.servicio.RentabilidadService.FiltroOrigen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RentabilidadPdfServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testGenerarPdfConResultadosCompletos() throws Exception {
        DetalleRentabilidad d1 = new DetalleRentabilidad(
                1, "Digital", "2026-08-15", "Cliente 1",
                new BigDecimal("100.00"), new BigDecimal("40.00"),
                new BigDecimal("60.00"), new BigDecimal("60.00"), true
        );
        DetalleRentabilidad d2 = new DetalleRentabilidad(
                2, "Papel", "2026-08-16", "Cliente 2",
                new BigDecimal("200.00"), new BigDecimal("50.00"),
                new BigDecimal("150.00"), new BigDecimal("75.00"), true
        );
        ResumenRentabilidad resumen = new ResumenRentabilidad(
                new BigDecimal("300.00"), new BigDecimal("90.00"),
                new BigDecimal("210.00"), new BigDecimal("70.00"),
                BigDecimal.ZERO, 0, 2, List.of(d1, d2)
        );

        File pdfFile = tempDir.resolve("Rentabilidad-2026-08.pdf").toFile();
        RentabilidadPdfService.generarPdf(resumen, 2026, 8, FiltroOrigen.TODAS, pdfFile);

        assertTrue(pdfFile.exists());

        try (PdfReader reader = new PdfReader(pdfFile.getAbsolutePath())) {
            assertEquals(1, reader.getNumberOfPages());
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String text = extractor.getTextFromPage(1);
            
            assertTrue(text.contains("Informe de Rentabilidad Mensual"));
            assertTrue(text.contains("Período analizado: 08/2026"));
            assertTrue(text.contains("Filtro aplicado: Todas"));
            assertTrue(text.contains("300,00")); // Facturación
            assertTrue(text.contains("90,00")); // Costo
            assertTrue(text.contains("210,00")); // Ganancia
            assertTrue(text.contains("70,00%")); // Margen
            assertTrue(text.contains("Cliente 1"));
            assertTrue(text.contains("Cliente 2"));
            assertFalse(text.contains("RESULTADO PARCIAL"));
        }
    }

    @Test
    void testGenerarPdfConResultadosParcialesYFiltro() throws Exception {
        DetalleRentabilidad d1 = new DetalleRentabilidad(
                1, "Digital", "2026-08-15", "Cliente Digital",
                new BigDecimal("100.00"), null, null, null, false
        );
        ResumenRentabilidad resumen = new ResumenRentabilidad(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                new BigDecimal("100.00"), 1, 0, List.of(d1)
        );

        File pdfFile = tempDir.resolve("Rentabilidad-Digitales.pdf").toFile();
        RentabilidadPdfService.generarPdf(resumen, 2026, 8, FiltroOrigen.DIGITALES, pdfFile);

        assertTrue(pdfFile.exists());

        try (PdfReader reader = new PdfReader(pdfFile.getAbsolutePath())) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String text = extractor.getTextFromPage(1);

            assertTrue(text.contains("Filtro aplicado: Digitales"));
            assertTrue(text.contains("RESULTADO PARCIAL: Hay operaciones sin costos conocidos"));
            assertTrue(text.contains("No calculable"));
            assertTrue(text.contains("No disponible"));
            assertTrue(text.contains("Incompleta"));
            assertTrue(text.contains("Cliente Digital"));
        }
    }

    @Test
    void testGenerarPdfSinOperaciones() throws Exception {
        ResumenRentabilidad resumen = new ResumenRentabilidad(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, 0, 0, Collections.emptyList()
        );

        File pdfFile = tempDir.resolve("Rentabilidad-Vacio.pdf").toFile();
        RentabilidadPdfService.generarPdf(resumen, 2026, 8, FiltroOrigen.TODAS, pdfFile);

        assertTrue(pdfFile.exists());
        
        try (PdfReader reader = new PdfReader(pdfFile.getAbsolutePath())) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String text = extractor.getTextFromPage(1);
            assertTrue(text.contains("Facturación Total del Mes: $0,00"));
        }
    }

    @Test
    void testGenerarPdfConPerdidas() throws Exception {
        DetalleRentabilidad d1 = new DetalleRentabilidad(
                1, "Papel", "2026-08-15", "Cliente Perdida",
                new BigDecimal("100.00"), new BigDecimal("150.00"),
                new BigDecimal("-50.00"), new BigDecimal("-50.00"), true
        );
        ResumenRentabilidad resumen = new ResumenRentabilidad(
                new BigDecimal("100.00"), new BigDecimal("150.00"),
                new BigDecimal("-50.00"), new BigDecimal("-50.00"),
                BigDecimal.ZERO, 0, 1, List.of(d1)
        );

        File pdfFile = tempDir.resolve("Rentabilidad-Perdida.pdf").toFile();
        RentabilidadPdfService.generarPdf(resumen, 2026, 8, FiltroOrigen.PAPEL, pdfFile);

        assertTrue(pdfFile.exists());
        
        try (PdfReader reader = new PdfReader(pdfFile.getAbsolutePath())) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String text = extractor.getTextFromPage(1);
            assertTrue(text.contains("-50,00"));
            assertTrue(text.contains("Cliente Perdida"));
        }
    }
    
    @Test
    void testManejoDeErrorIO() {
        ResumenRentabilidad resumen = new ResumenRentabilidad(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, 0, 0, Collections.emptyList()
        );

        // Pasamos un directorio en lugar de un archivo para forzar una excepción de escritura
        File dirComoArchivo = tempDir.toFile();
        
        Exception ex = assertThrows(Exception.class, () -> {
            RentabilidadPdfService.generarPdf(resumen, 2026, 8, FiltroOrigen.TODAS, dirComoArchivo);
        });
        
        assertTrue(ex.getMessage().contains("Error al generar o guardar el informe PDF"));
    }
}
