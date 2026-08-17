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

    private static final float ANCHO_DOCUMENTO = PageSize.A5.getWidth();
    private static final float ALTURA_MAXIMA = PageSize.A4.getHeight();
    private static final float MARGEN_HORIZONTAL = 15f;
    private static final float MARGEN_VERTICAL = 12f;

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

        float altura = calcularAlturaReal(codigoUnico, fechaEmisionLocal, fechaLimite,
                dniCliente, nombreCliente, items, totalEstimado, emisor);
        Rectangle hoja = new Rectangle(ANCHO_DOCUMENTO, Math.min(altura, ALTURA_MAXIMA));
        Document document = new Document(hoja, MARGEN_HORIZONTAL, MARGEN_HORIZONTAL,
                MARGEN_VERTICAL, MARGEN_VERTICAL);

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
            header.setWidths(new float[]{25f, 45f, 30f});
            header.setSpacingAfter(8f);

            PdfPCell cellLogo = cellSinBorde();
            agregarLogo(cellLogo, fTitle);
            header.addCell(cellLogo);

            PdfPCell cellEmisor = cellSinBorde();
            if (emisor != null) {
                cellEmisor.addElement(new Paragraph("Emisor: " + emisor.nombre(), fBody8));
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

    private static float calcularAlturaReal(String codigoUnico, String fechaEmisionLocal,
                                             String fechaLimite, String dniCliente,
                                             String nombreCliente, List<ItemPresupuesto> items,
                                             BigDecimal totalEstimado,
                                             io.github.ramiro.escapesj.modelo.Emisor emisor) {
        Font bold = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font body = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Font title = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font tiny = new Font(Font.HELVETICA, 6, Font.NORMAL);
        Font totalFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font code = new Font(Font.COURIER, 10, Font.BOLD);
        float anchoUtil = ANCHO_DOCUMENTO - (MARGEN_HORIZONTAL * 2);

        PdfPTable titulo = new PdfPTable(1);
        titulo.addCell(celdaMedicion("PRESUPUESTO", title));

        PdfPTable header = new PdfPTable(new float[]{25f, 45f, 30f});
        PdfPCell logo = cellSinBorde();
        agregarLogo(logo, title);
        header.addCell(logo);
        PdfPCell datos = cellSinBorde();
        if (emisor != null) {
            datos.addElement(new Paragraph("Emisor: " + emisor.nombre(), body));
            datos.addElement(new Paragraph("CUIT Emisor: " + emisor.cuit(), body));
            datos.addElement(new Paragraph("Lugar Emisión: " + (emisor.calle() != null ? emisor.calle() : "Viedma, Rio Negro"), body));
            if (emisor.telefono() != null && !emisor.telefono().isEmpty()) {
                datos.addElement(new Paragraph("Teléfono Atención: " + emisor.telefono(), body));
            }
        }
        header.addCell(datos);
        PdfPCell info = cellSinBorde();
        info.addElement(new Paragraph("Código: " + codigoUnico, code));
        info.addElement(new Paragraph("Fecha: " + fechaEmisionLocal, body));
        header.addCell(info);

        PdfPTable cliente = new PdfPTable(1);
        cliente.addCell(celdaMedicion("Cliente: " + nombreCliente + "\nDNI: " + dniCliente, body));

        PdfPTable tablaItems = new PdfPTable(new float[]{3.5f, 0.8f, 1.3f, 1.3f});
        for (String texto : new String[]{"Descripción", "Cant.", "Precio", "Importe"}) {
            PdfPCell celda = new PdfPCell(new Paragraph(texto, bold));
            celda.setPadding(2f);
            tablaItems.addCell(celda);
        }
        for (ItemPresupuesto item : items) {
            tablaItems.addCell(celda(item.descripcion(), body));
            tablaItems.addCell(celda(String.valueOf(item.cantidad()), body));
            tablaItems.addCell(celda(String.format("$%,.2f", item.precioUnitario()), body));
            tablaItems.addCell(celda(String.format("$%,.2f", item.subtotal()), body));
        }

        PdfPTable pie = new PdfPTable(1);
        PdfPCell bloque = cellSinBorde();
        bloque.addElement(new Paragraph(String.format("TOTAL ESTIMADO: $%,.2f", totalEstimado), totalFont));
        bloque.addElement(new Paragraph("Presupuesto válido hasta: "
                + io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(fechaLimite), bold));
        bloque.addElement(new Paragraph("Este presupuesto garantiza el precio indicado hasta la fecha de validez. "
                + "Pasada dicha fecha, el monto podrá ser actualizado.", tiny));
        bloque.addElement(new Paragraph("Código de verificación: " + codigoUnico, body));
        pie.addCell(bloque);

        float contenido = medir(titulo, anchoUtil) + medir(header, anchoUtil)
                + medir(cliente, anchoUtil) + medir(tablaItems, anchoUtil) + medir(pie, anchoUtil);
        return Math.max(340f, contenido + (MARGEN_VERTICAL * 2) + 70f);
    }

    private static PdfPCell celdaMedicion(String texto, Font font) {
        PdfPCell celda = new PdfPCell(new Paragraph(texto, font));
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(1f);
        return celda;
    }

    private static float medir(PdfPTable tabla, float ancho) {
        tabla.setTotalWidth(ancho);
        tabla.setLockedWidth(true);
        tabla.calculateHeights(true);
        return tabla.getTotalHeight();
    }

    private static void agregarLogo(PdfPCell cell, Font fallbackFont) {
        try {
            URL logoUrl = PresupuestoPdfService.class.getResource("/Logo.png");
            if (logoUrl != null) {
                Image logo = Image.getInstance(logoUrl);
                logo.scaleToFit(65, 65);
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
