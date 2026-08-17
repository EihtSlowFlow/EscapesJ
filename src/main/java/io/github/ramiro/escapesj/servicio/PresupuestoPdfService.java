package io.github.ramiro.escapesj.servicio;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;

/**
 * Genera presupuestos en PDF A4, con código único, múltiples ítems y fecha de validez.
 */
public class PresupuestoPdfService {

    public record ItemPresupuesto(String descripcion, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

    public static String generarPdf(String codigoUnico, String fechaEmision, String fechaLimite,
                                     String dniCliente, String nombreCliente,
                                     List<ItemPresupuesto> items, BigDecimal totalEstimado,
                                     String carpetaDestino, io.github.ramiro.escapesj.modelo.Emisor emisor) {

        if (carpetaDestino == null || carpetaDestino.isEmpty()) {
            carpetaDestino = io.github.ramiro.escapesj.persistencia.ConfigRepository.getDefaultPresupuestosPath();
        }

        File dir = new File(carpetaDestino);
        if (!dir.exists()) dir.mkdirs();

        String fechaEmisionLocal = io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(fechaEmision);
        String fileName = String.format("Presupuesto_%s_%s.pdf",
                codigoUnico, fechaEmisionLocal.replace("/", "-"));
        String filePath = new File(dir, fileName).getAbsolutePath();

        Document document = new Document(PageSize.A4, 28, 28, 24, 24);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Font fBold10 = new Font(Font.HELVETICA, 9, Font.BOLD);
            Font fBody8 = new Font(Font.HELVETICA, 8, Font.NORMAL);
            Font fTitle = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font fTiny = new Font(Font.HELVETICA, 6, Font.NORMAL);
            Font fTotal = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font fCode = new Font(Font.COURIER, 10, Font.BOLD);

            // ── TÍTULO ──
            Paragraph titulo = new Paragraph("PRESUPUESTO", fTitle);
            titulo.setAlignment(Paragraph.ALIGN_CENTER);
            titulo.setSpacingAfter(3f);
            document.add(titulo);

            // ── CABECERA: Logo + Emisor + Código/fecha ──
            PdfPTable header = new PdfPTable(3);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.1f, 3.5f, 1.7f});
            header.setSpacingAfter(8f);

            PdfPCell cellLogo = cellSinBorde();
            agregarLogo(cellLogo, fTitle);
            header.addCell(cellLogo);

            PdfPCell cellEmisor = cellSinBorde();
            if (emisor != null) {
                cellEmisor.addElement(new Paragraph(emisor.nombre(), fTitle));
                cellEmisor.addElement(new Paragraph("CUIT Emisor: " + emisor.cuit(), fBody8));
                cellEmisor.addElement(new Paragraph("Lugar Emisión: " + (emisor.calle() != null ? emisor.calle() : "Viedma, Rio Negro"), fBody8));
                if (emisor.telefono() != null && !emisor.telefono().isEmpty()) {
                    cellEmisor.addElement(new Paragraph("Teléfono Atención: " + emisor.telefono(), fBody8));
                }
            }
            header.addCell(cellEmisor);

            PdfPCell cellInfo = cellSinBorde();
            cellInfo.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
            Paragraph codP = new Paragraph("Código: " + codigoUnico, fCode);
            codP.setAlignment(Paragraph.ALIGN_RIGHT);
            Paragraph fechaP = new Paragraph("Fecha: " + fechaEmisionLocal, fBody8);
            fechaP.setAlignment(Paragraph.ALIGN_RIGHT);
            cellInfo.addElement(codP);
            cellInfo.addElement(fechaP);
            header.addCell(cellInfo);
            document.add(header);

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
            tabla.setHeaderRows(1);
            tabla.setSplitLate(false);

            for (String h : new String[]{"Descripción", "Cant.", "Precio", "Importe"}) {
                PdfPCell c = new PdfPCell(new Paragraph(h, fBold10));
                c.setBorder(Rectangle.BOTTOM);
                c.setPadding(2f);
                c.setPaddingBottom(3f);
                tabla.addCell(c);
            }

            for (ItemPresupuesto item : items) {
                tabla.addCell(celda(item.descripcion(), fBody8));
                tabla.addCell(celda(String.valueOf(item.cantidad()), fBody8));
                tabla.addCell(celda(String.format("$%,.2f", item.precioUnitario()), fBody8));
                tabla.addCell(celda(String.format("$%,.2f", item.subtotal()), fBody8));
            }

            PdfPCell linea = new PdfPCell(new Paragraph(" ", fTiny));
            linea.setColspan(4);
            linea.setBorder(Rectangle.TOP);
            linea.setFixedHeight(2f);
            tabla.addCell(linea);

            tabla.setSpacingAfter(4f);
            document.add(tabla);

            // ── TOTAL ──
            Paragraph totalP = new Paragraph(String.format("TOTAL ESTIMADO: $%,.2f", totalEstimado), fTotal);
            totalP.setAlignment(Paragraph.ALIGN_RIGHT);
            totalP.setSpacingAfter(6f);
            document.add(totalP);

            // ── VALIDEZ ──
            Paragraph validezP = new Paragraph(
                    "✓ Presupuesto válido hasta: " + io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(fechaLimite), fBold10);
            validezP.setSpacingAfter(2f);
            document.add(validezP);

            Paragraph notaP = new Paragraph(
                    "Este presupuesto garantiza el precio indicado hasta la fecha de validez. " +
                    "Pasada dicha fecha, el monto podrá ser actualizado.", fTiny);
            notaP.setSpacingAfter(4f);
            document.add(notaP);

            // ── PIE ──
            Paragraph pieP = new Paragraph("Código de verificación: " + codigoUnico, fBody8);
            document.add(pieP);

            document.close();
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de presupuesto: " + e.getMessage(), e);
        }
    }

    private static void agregarLogo(PdfPCell cell, Font fallbackFont) {
        try {
            URL logoUrl = PresupuestoPdfService.class.getResource("/Logo.png");
            if (logoUrl != null) {
                Image logo = Image.getInstance(logoUrl);
                logo.scaleToFit(60, 60);
                cell.addElement(logo);
                return;
            }
        } catch (Exception ignored) {
            // Se usa el texto de respaldo si el recurso no puede cargarse.
        }
        cell.addElement(new Paragraph("escapesJ", fallbackFont));
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
