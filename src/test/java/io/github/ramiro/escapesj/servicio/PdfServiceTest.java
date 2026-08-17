package io.github.ramiro.escapesj.servicio;

import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.github.ramiro.escapesj.modelo.Emisor;
import io.github.ramiro.escapesj.persistencia.BoletaRepository.BoletaItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {

    @TempDir
    Path tempDir;

    private final Emisor emisor = new Emisor(
            1, "Escapes del Sur", "20123456789", "Mitre 123", "2920123456");

    @Test
    void generaBoletaConDatosDelEmisorYFechaLocal() throws Exception {
        String ruta = BoletaPdfService.generarPdf(
                7, "2026-07-31", "12345678", "Cliente",
                List.of(new BoletaItem(1, "SERVICIO", "Soldadura", null, 1,
                        new BigDecimal("1500.00"), new BigDecimal("1500.00"), BigDecimal.ZERO)),
                new BigDecimal("1500.00"), "TRANSFERENCIA", BigDecimal.ZERO,
                tempDir.toString(), emisor);

        Path pdf = Path.of(ruta);
        assertEquals("Boleta_0007_31-07-2026.pdf", pdf.getFileName().toString());
        assertTrue(Files.isRegularFile(pdf));

        String contenido = extraerTexto(pdf);
        assertTrue(contenido.contains("31/07/2026"), contenido);
        assertTrue(contenido.contains("Escapes del Sur"), contenido);
        assertTrue(contenido.contains("CUIT Emisor: 20123456789"), contenido);
        assertTrue(contenido.contains("Lugar Emisión: Mitre 123"), contenido);
    }

    @Test
    void generaBoletaRepresentativaA4EnUnaPaginaConLogoYPieCompleto() throws Exception {
        List<BoletaItem> items = List.of(
                item("Silenciador", "1000.00"),
                item("Colocación", "500.00"),
                item("Abrazadera", "250.00"));

        Path pdf = Path.of(BoletaPdfService.generarPdf(
                8, "2026-08-17", "12345678", "Cliente",
                items, new BigDecimal("1750.00"), "EFECTIVO", new BigDecimal("10"),
                tempDir.toString(), emisor));

        PdfReader reader = new PdfReader(pdf.toString());
        try {
            assertEquals(1, reader.getNumberOfPages());
            Rectangle pagina = reader.getPageSize(1);
            assertEquals(PageSize.A4.getWidth(), pagina.getWidth(), 0.1f);
            assertEquals(PageSize.A4.getHeight(), pagina.getHeight(), 0.1f);

            String primeraPagina = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(primeraPagina.contains("EFECTIVO"), primeraPagina);
            assertTrue(primeraPagina.contains("Cond. Venta"), primeraPagina);
            assertTrue(contieneImagen(reader), "La boleta debe incluir Logo.png como recurso de imagen");
        } finally {
            reader.close();
        }
    }

    @Test
    void divideBoletaConMuchosItemsEnPaginasA4() throws Exception {
        List<BoletaItem> items = IntStream.rangeClosed(1, 100)
                .mapToObj(numero -> item("Repuesto " + numero, "100.00"))
                .toList();

        Path pdf = Path.of(BoletaPdfService.generarPdf(
                9, "2026-08-17", "12345678", "Cliente",
                items, new BigDecimal("10000.00"), "TRANSFERENCIA", BigDecimal.ZERO,
                tempDir.toString(), emisor));

        PdfReader reader = new PdfReader(pdf.toString());
        try {
            assertTrue(reader.getNumberOfPages() > 1);
            for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
                Rectangle tamanio = reader.getPageSize(pagina);
                assertEquals(PageSize.A4.getWidth(), tamanio.getWidth(), 0.1f);
                assertEquals(PageSize.A4.getHeight(), tamanio.getHeight(), 0.1f);
            }
            String ultimaPagina = new PdfTextExtractor(reader).getTextFromPage(reader.getNumberOfPages());
            assertTrue(ultimaPagina.contains("Repuesto 100"), ultimaPagina);
            assertTrue(ultimaPagina.contains("TOTAL"), ultimaPagina);
            assertTrue(ultimaPagina.contains("Cond. Venta"), ultimaPagina);
        } finally {
            reader.close();
        }
    }

    @Test
    void generaPresupuestoConDatosDelEmisorYFechasLocales() throws Exception {
        String ruta = PresupuestoPdfService.generarPdf(
                "P-ABC123", "2026-07-31", "2026-08-15", "12345678", "Cliente",
                List.of(new PresupuestoPdfService.ItemPresupuesto(
                        "Silenciador", 1, new BigDecimal("2000.00"), new BigDecimal("2000.00"))),
                new BigDecimal("2000.00"), tempDir.toString(), emisor);

        Path pdf = Path.of(ruta);
        assertEquals("Presupuesto_P-ABC123_31-07-2026.pdf", pdf.getFileName().toString());
        assertTrue(Files.isRegularFile(pdf));

        String contenido = extraerTexto(pdf);
        assertTrue(contenido.contains("Fecha: 31/07/2026"), contenido);
        assertTrue(contenido.contains("15/08/2026"), contenido);
        assertTrue(contenido.contains("CUIT Emisor: 20123456789"), contenido);
        assertTrue(contenido.contains("Lugar Emisión: Mitre 123"), contenido);

        PdfReader reader = new PdfReader(pdf.toString());
        try {
            Rectangle pagina = reader.getPageSize(1);
            assertEquals(PageSize.A4.getWidth(), pagina.getWidth(), 0.1f);
            assertEquals(PageSize.A4.getHeight(), pagina.getHeight(), 0.1f);
            assertTrue(contieneImagen(reader), "El presupuesto debe incluir Logo.png");
        } finally {
            reader.close();
        }
    }

    private String extraerTexto(Path pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf.toString());
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder contenido = new StringBuilder();
            for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
                contenido.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return contenido.toString();
        } finally {
            reader.close();
        }
    }

    private BoletaItem item(String descripcion, String importe) {
        BigDecimal monto = new BigDecimal(importe);
        return new BoletaItem(1, "SERVICIO", descripcion, null, 1, monto, monto, BigDecimal.ZERO);
    }

    private boolean contieneImagen(PdfReader reader) {
        for (int indice = 0; indice < reader.getXrefSize(); indice++) {
            PdfObject objeto = reader.getPdfObject(indice);
            if (objeto instanceof PdfDictionary diccionario
                    && PdfName.XOBJECT.equals(diccionario.getAsName(PdfName.TYPE))
                    && PdfName.IMAGE.equals(diccionario.getAsName(PdfName.SUBTYPE))) {
                return true;
            }
        }
        return false;
    }
}
