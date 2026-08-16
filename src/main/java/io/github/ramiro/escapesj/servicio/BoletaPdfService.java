package io.github.ramiro.escapesj.servicio;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import io.github.ramiro.escapesj.persistencia.BoletaRepository.BoletaItem;
import io.github.ramiro.escapesj.modelo.Emisor;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;

/**
 * Genera boletas en PDF compactas, ajustadas al contenido, listas para imprimir.
 */
public class BoletaPdfService {

    /**
     * @param metodoPago       "EFECTIVO" o "TRANSFERENCIA"
     * @param descuentoPorcentaje  porcentaje de descuento por efectivo (ej: 10.0)
     */
    public static String generarPdf(int numeroBoleta, String fecha, String dniCliente,
                                     String nombreCliente,
                                     List<BoletaItem> items,
                                     BigDecimal subtotal,
                                     String metodoPago, BigDecimal descuentoPorcentaje,
                                     String carpetaDestino, Emisor emisor) {

        if (carpetaDestino == null || carpetaDestino.isEmpty()) {
            carpetaDestino = io.github.ramiro.escapesj.persistencia.ConfigRepository.getDefaultBoletasPath();
        }

        java.io.File dir = new java.io.File(carpetaDestino);
        if (!dir.exists()) dir.mkdirs();

        String fechaLocal = io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(fecha);
        String fileName = String.format("Boleta_%04d_%s.pdf", numeroBoleta, fechaLocal.replace("/", "-"));
        String filePath = new java.io.File(dir, fileName).getAbsolutePath();

        // Altura dinámica ajustada al contenido
        //  Logo+cabecera: ~90pt, cliente: 25pt, tabla header: 18pt, por ítem: 15pt, totales+pie: ~80pt
        float alturaContenido = 90f + 25f + 18f + (items.size() * 15f) + 80f;
        if (descuentoPorcentaje.compareTo(BigDecimal.ZERO) > 0) alturaContenido += 30f; // descuento + línea EFECTIVO
        alturaContenido = Math.max(alturaContenido, 220f);
        Rectangle pagesize = new Rectangle(454f, alturaContenido);
        Document document = new Document(pagesize, 15, 15, 10, 8);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Font fBold10 = new Font(Font.HELVETICA, 9, Font.BOLD);
            Font fBody8 = new Font(Font.HELVETICA, 8, Font.NORMAL);
            Font fTitle = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font fTiny = new Font(Font.HELVETICA, 6, Font.NORMAL);
            Font fTotal = new Font(Font.HELVETICA, 10, Font.BOLD);

            // ── AVISO ──
            Paragraph aviso = new Paragraph("DOCUMENTO NO VÁLIDO COMO FACTURA", fTiny);
            aviso.setAlignment(Paragraph.ALIGN_CENTER);
            aviso.setSpacingAfter(3f);
            document.add(aviso);

            // ── CABECERA: Logo + Número ──
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1f, 1f});
            header.setSpacingAfter(4f);

            PdfPCell cellLogo = cellSinBorde();
            if (emisor != null && emisor.nombre() != null) {
                cellLogo.addElement(new Paragraph(emisor.nombre(), fTitle));
            } else {
                try {
                    URL logoUrl = BoletaPdfService.class.getResource("/Logo.png");
                    if (logoUrl != null) {
                        Image logo = Image.getInstance(logoUrl);
                        logo.scaleToFit(60, 60);
                        cellLogo.addElement(logo);
                    } else {
                        cellLogo.addElement(new Paragraph("escapesJ", fTitle));
                    }
                } catch (Exception e) {
                    cellLogo.addElement(new Paragraph("escapesJ", fTitle));
                }
            }
            header.addCell(cellLogo);

            PdfPCell cellNum = cellSinBorde();
            cellNum.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
            Paragraph numP = new Paragraph(String.format("0001-%08d", numeroBoleta), fBold10);
            numP.setAlignment(PdfPCell.ALIGN_RIGHT);
            Paragraph fechaP = new Paragraph(fechaLocal, fBody8);
            fechaP.setAlignment(PdfPCell.ALIGN_RIGHT);
            cellNum.addElement(numP);
            cellNum.addElement(fechaP);
            header.addCell(cellNum);
            document.add(header);
            document.add(new Paragraph(" "));

            // ── DATOS DEL EMISOR (Cabecera) ──
            if (emisor != null) {
                PdfPTable tableEmisor = new PdfPTable(1);
                tableEmisor.setWidthPercentage(100);
                PdfPCell cellEmisor = cellSinBorde();
                cellEmisor.addElement(new Paragraph("Atendido por: " + emisor.nombre(), fBody8));
                cellEmisor.addElement(new Paragraph("CUIT Emisor: " + emisor.cuit(), fBody8));
                cellEmisor.addElement(new Paragraph("Lugar Emisión: " + (emisor.calle() != null ? emisor.calle() : "Viedma, Rio Negro"), fBody8));
                if (emisor.telefono() != null && !emisor.telefono().isEmpty()) {
                    cellEmisor.addElement(new Paragraph("Teléfono Atención: " + emisor.telefono(), fBody8));
                }
                tableEmisor.addCell(cellEmisor);
                document.add(tableEmisor);
                document.add(new Paragraph(" "));
            }



            // ── CLIENTE ──
            Paragraph clienteP = new Paragraph("Cliente: " + nombreCliente, fBody8);
            clienteP.setSpacingAfter(1f);
            document.add(clienteP);
            Paragraph dniP = new Paragraph("DNI: " + dniCliente, fBody8);
            dniP.setSpacingAfter(4f);
            document.add(dniP);

            // ── TABLA DE ÍTEMS ──
            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{3.5f, 0.8f, 1.3f, 1.3f});

            // Headers
            for (String h : new String[]{"Descripción", "Cant.", "Precio", "Importe"}) {
                PdfPCell c = new PdfPCell(new Paragraph(h, fBold10));
                c.setBorder(Rectangle.BOTTOM);
                c.setPadding(2f);
                c.setPaddingBottom(3f);
                tabla.addCell(c);
            }

            // Filas
            for (BoletaItem item : items) {
                tabla.addCell(celda(item.descripcion(), fBody8));
                tabla.addCell(celda(String.valueOf(item.cantidad()), fBody8));
                tabla.addCell(celda(String.format("$%,.2f", item.precioUnitario()), fBody8));
                tabla.addCell(celda(String.format("$%,.2f", item.subtotal()), fBody8));
            }

            // Línea separadora
            PdfPCell linea = new PdfPCell(new Paragraph(" ", fTiny));
            linea.setColspan(4);
            linea.setBorder(Rectangle.TOP);
            linea.setFixedHeight(2f);
            tabla.addCell(linea);

            tabla.setSpacingAfter(3f);
            document.add(tabla);

            // ── TOTALES ──
            boolean esEfectivo = "EFECTIVO".equalsIgnoreCase(metodoPago);
            BigDecimal descuento = esEfectivo ? io.github.ramiro.escapesj.sdk.DineroUtil.redondearMoneda(subtotal.multiply(descuentoPorcentaje).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)) : BigDecimal.ZERO;
            BigDecimal totalFinal = subtotal.subtract(descuento);

            // Subtotal
            Paragraph subP = new Paragraph(String.format("Subtotal: $%,.2f", subtotal), fBody8);
            subP.setAlignment(Paragraph.ALIGN_RIGHT);
            subP.setSpacingAfter(1f);
            document.add(subP);

            // Descuento (solo si efectivo)
            if (esEfectivo && descuento.compareTo(BigDecimal.ZERO) > 0) {
                Paragraph dtoP = new Paragraph(
                        String.format("DTO: -%s%% = -$%,.2f", descuentoPorcentaje.toString(), descuento), fBody8);
                dtoP.setAlignment(Paragraph.ALIGN_RIGHT);
                dtoP.setSpacingAfter(1f);
                document.add(dtoP);
            }

            // Total final
            Paragraph totalP = new Paragraph(String.format("TOTAL: $%,.2f", totalFinal), fTotal);
            totalP.setAlignment(Paragraph.ALIGN_RIGHT);
            totalP.setSpacingAfter(2f);
            document.add(totalP);

            // ── PIE ──
            String condVenta = esEfectivo
                    ? "Cond. Venta: Contado (Efectivo)"
                    : "Cond. Venta: Contado (Transferencia)";
            Paragraph cond = new Paragraph(condVenta, fBody8);

            if (esEfectivo && descuento.compareTo(BigDecimal.ZERO) > 0) {
                Paragraph pagoP = new Paragraph(
                        String.format("EFECTIVO: $%,.2f", totalFinal), fBold10);
                pagoP.setAlignment(Paragraph.ALIGN_RIGHT);
                pagoP.setSpacingAfter(2f);
                document.add(pagoP);
            }
            document.add(cond);

            document.close();
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    private static PdfPCell cellSinBorde() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1f);
        return c;
    }

    private static PdfPCell celda(String texto, Font font) {
        PdfPCell c = new PdfPCell(new Paragraph(texto, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1f);
        c.setPaddingBottom(2f);
        return c;
    }
}
