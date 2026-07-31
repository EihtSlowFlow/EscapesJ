package io.github.ramiro.escapesj.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GeneradorOrdenCustom {
    private static final Logger logger = LoggerFactory.getLogger(GeneradorOrdenCustom.class);


    // Helper para formatear números: $16,017.61
    private String formatDinero(java.math.BigDecimal valor) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("$#,##0.00", symbols);
        return df.format(valor);
    }

    public void generarOrdenFinal(
            String idUnicoBD,
            String nombreCliente,
            String domicilioSDK,
            String cuitSDK,
            String condVentaSwing,
            String nroSDK,
            String producto,
            String cantidad,
            java.math.BigDecimal subtotal,
            java.math.BigDecimal porcentajeDto,
            String metodoPago) {

        java.math.BigDecimal montoDescontado = subtotal.multiply(porcentajeDto).divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal totalFinal = subtotal.subtract(montoDescontado);

        // Tamaño 16x9 cm
        Rectangle customSize = new Rectangle(453.6f, 255.15f);
        Document document = new Document(customSize);

        try {
            PdfWriter.getInstance(document, new FileOutputStream("Orden_" + idUnicoBD + ".pdf"));
            document.setMargins(15, 15, 10, 10);
            document.open();

            // --- 1. HEADER ---
            PdfPTable headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1.5f, 3f, 2f});
            headerTable.addCell(createCellNoBorder("LOGO\nescapesJ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));

            Font fAviso = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            PdfPCell cellAviso = new PdfPCell(new Phrase("DOCUMENTO NO VÁLIDO\nCOMO FACTURA", fAviso));
            cellAviso.setBackgroundColor(Color.BLACK);
            cellAviso.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerTable.addCell(cellAviso);

            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            PdfPCell cellNro = new PdfPCell(new Phrase(nroSDK + "\n" + fecha, FontFactory.getFont(FontFactory.HELVETICA, 8)));
            cellNro.setBorder(Rectangle.NO_BORDER);
            cellNro.setHorizontalAlignment(Element.ALIGN_RIGHT);
            headerTable.addCell(cellNro);
            document.add(headerTable);

            // --- 2. DATOS CLIENTE ---
            Font fLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font fValue = FontFactory.getFont(FontFactory.HELVETICA, 9);
            document.add(new Paragraph("Cliente: (" + idUnicoBD + ") - " + nombreCliente, fValue));
            document.add(new Paragraph("Domicilio: " + domicilioSDK, fValue));

            PdfPTable fTable = new PdfPTable(3);
            fTable.setWidthPercentage(100);
            fTable.addCell(createCellNoBorder("CUIT: " + cuitSDK, fValue));
            fTable.addCell(createCellNoBorder("I.V.A.: Consumidor Final", fValue));
            fTable.addCell(createCellNoBorder("Cond. Venta: " + condVentaSwing, fValue));
            document.add(fTable);

            // --- 3. TABLA PRODUCTOS ---
            PdfPTable itemTable = new PdfPTable(5);
            itemTable.setWidthPercentage(100);
            itemTable.setSpacingBefore(5);
            itemTable.setWidths(new float[]{3f, 1f, 1.2f, 1f, 1.2f});

            String[] heads = {"producto", "cantidad", "precio", "desc.", "importe"};
            for (String h : heads) {
                PdfPCell c = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                c.setBackgroundColor(Color.LIGHT_GRAY);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemTable.addCell(c);
            }

            itemTable.addCell(new Phrase(producto, fValue));
            itemTable.addCell(new Phrase(cantidad, fValue));
            itemTable.addCell(new Phrase(formatDinero(subtotal), fValue));
            itemTable.addCell(new Phrase(porcentajeDto + "%", fValue));
            itemTable.addCell(new Phrase(formatDinero(totalFinal), fValue));
            document.add(itemTable);

            // --- 4. TOTALES (BAJADOS A LOS CÍRCULOS) ---
            PdfPTable foot = new PdfPTable(2);
            foot.setWidthPercentage(100);

            // Aumentamos este valor para empujar la tabla hacia abajo (hacia los círculos)
            foot.setSpacingBefore(45f);

            // Izquierda (Círculo Verde)
            PdfPCell cPago = new PdfPCell(new Phrase(metodoPago.toUpperCase() + " : " + formatDinero(totalFinal), fLabel));
            cPago.setBorder(Rectangle.NO_BORDER);
            cPago.setVerticalAlignment(Element.ALIGN_BOTTOM);
            foot.addCell(cPago);

            // Derecha (Círculo Azul)
            Paragraph desglose = new Paragraph();
            desglose.add(new Chunk("Subtotal: " + formatDinero(subtotal) + "\n", fLabel));
            desglose.add(new Chunk("DTO: -" + porcentajeDto + "% = -" + formatDinero(montoDescontado) + "\n", fLabel));
            desglose.add(new Chunk("TOTAL: " + formatDinero(totalFinal), fLabel));

            PdfPCell cDesglose = new PdfPCell(desglose);
            cDesglose.setBorder(Rectangle.NO_BORDER);
            cDesglose.setHorizontalAlignment(Element.ALIGN_RIGHT);
            foot.addCell(cDesglose);

            document.add(foot);
            document.close();

        } catch (Exception e) {
            logger.error("Error:", e);
        }
    }

    private PdfPCell createCellNoBorder(String t, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(t, f));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }


}