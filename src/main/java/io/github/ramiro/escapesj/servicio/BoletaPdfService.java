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

/** Genera boletas en formato ticket listas para imprimir. */
public class BoletaPdfService {

    private static final float ANCHO_BOLETA = PageSize.A5.getWidth();
    private static final float ALTURA_MAXIMA = PageSize.A4.getHeight();
    private static final float MARGEN_HORIZONTAL = 18f;
    private static final float MARGEN_VERTICAL = 14f;

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

        float alturaTicket = calcularAlturaReal(items, nombreCliente, dniCliente, emisor,
                subtotal, metodoPago, descuentoPorcentaje, numeroBoleta, fechaLocal);
        Document document = new Document(
                new Rectangle(ANCHO_BOLETA, Math.min(alturaTicket, ALTURA_MAXIMA)),
                MARGEN_HORIZONTAL, MARGEN_HORIZONTAL, MARGEN_VERTICAL, MARGEN_VERTICAL);

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

            // ── CABECERA: Logo + Emisor + Número/fecha ──
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

            PdfPTable tablaTotales = new PdfPTable(1);
            tablaTotales.setWidthPercentage(100);
            tablaTotales.setKeepTogether(true);
            PdfPCell bloqueTotales = cellSinBorde();

            // Subtotal
            Paragraph subP = new Paragraph(String.format("Subtotal: $%,.2f", subtotal), fBody8);
            subP.setAlignment(Paragraph.ALIGN_RIGHT);
            subP.setSpacingAfter(1f);
            bloqueTotales.addElement(subP);

            // Descuento (solo si efectivo)
            if (esEfectivo && descuento.compareTo(BigDecimal.ZERO) > 0) {
                Paragraph dtoP = new Paragraph(
                        String.format("DTO: -%s%% = -$%,.2f", descuentoPorcentaje.toString(), descuento), fBody8);
                dtoP.setAlignment(Paragraph.ALIGN_RIGHT);
                dtoP.setSpacingAfter(1f);
                bloqueTotales.addElement(dtoP);
            }

            // Total final
            Paragraph totalP = new Paragraph(String.format("TOTAL: $%,.2f", totalFinal), fTotal);
            totalP.setAlignment(Paragraph.ALIGN_RIGHT);
            totalP.setSpacingAfter(2f);
            bloqueTotales.addElement(totalP);

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
                bloqueTotales.addElement(pagoP);
            }
            bloqueTotales.addElement(cond);
            tablaTotales.addCell(bloqueTotales);
            document.add(tablaTotales);

            document.close();
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    private static float calcularAlturaReal(List<BoletaItem> items, String nombreCliente,
                                             String dniCliente, Emisor emisor,
                                             BigDecimal subtotal, String metodoPago,
                                             BigDecimal descuentoPorcentaje,
                                             int numeroBoleta, String fechaLocal) {
        Font bold = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font body = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Font tiny = new Font(Font.HELVETICA, 6, Font.NORMAL);
        Font totalFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        float anchoUtil = ANCHO_BOLETA - (MARGEN_HORIZONTAL * 2);

        PdfPTable aviso = new PdfPTable(1);
        aviso.addCell(celdaMedicion("DOCUMENTO NO VÁLIDO COMO FACTURA", tiny));

        PdfPTable header = new PdfPTable(new float[]{25f, 45f, 30f});
        PdfPCell logo = cellSinBorde();
        agregarLogo(logo, bold);
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
        PdfPCell numero = cellSinBorde();
        numero.addElement(new Paragraph(String.format("0001-%08d", numeroBoleta), bold));
        numero.addElement(new Paragraph(fechaLocal, body));
        header.addCell(numero);

        PdfPTable cliente = new PdfPTable(1);
        cliente.addCell(celdaMedicion("Cliente: " + nombreCliente + "\nDNI: " + dniCliente, body));

        PdfPTable tablaItems = new PdfPTable(new float[]{3.5f, 0.8f, 1.3f, 1.3f});
        for (String texto : new String[]{"Descripción", "Cant.", "Precio", "Importe"}) {
            PdfPCell celda = new PdfPCell(new Paragraph(texto, bold));
            celda.setPadding(2f);
            tablaItems.addCell(celda);
        }
        for (BoletaItem item : items) {
            tablaItems.addCell(celda(item.descripcion(), body));
            tablaItems.addCell(celda(String.valueOf(item.cantidad()), body));
            tablaItems.addCell(celda(String.format("$%,.2f", item.precioUnitario()), body));
            tablaItems.addCell(celda(String.format("$%,.2f", item.subtotal()), body));
        }

        boolean efectivo = "EFECTIVO".equalsIgnoreCase(metodoPago);
        BigDecimal descuento = efectivo
                ? io.github.ramiro.escapesj.sdk.DineroUtil.redondearMoneda(subtotal.multiply(descuentoPorcentaje).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP))
                : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(descuento);
        PdfPTable totales = new PdfPTable(1);
        PdfPCell bloque = cellSinBorde();
        bloque.addElement(new Paragraph(String.format("Subtotal: $%,.2f", subtotal), body));
        if (efectivo && descuento.signum() > 0) {
            bloque.addElement(new Paragraph(String.format("DTO: -%s%% = -$%,.2f", descuentoPorcentaje, descuento), body));
        }
        bloque.addElement(new Paragraph(String.format("TOTAL: $%,.2f", total), totalFont));
        if (efectivo && descuento.signum() > 0) {
            bloque.addElement(new Paragraph(String.format("EFECTIVO: $%,.2f", total), bold));
        }
        bloque.addElement(new Paragraph(efectivo
                ? "Cond. Venta: Contado (Efectivo)"
                : "Cond. Venta: Contado (Transferencia)", body));
        totales.addCell(bloque);

        float contenido = medir(aviso, anchoUtil) + medir(header, anchoUtil)
                + medir(cliente, anchoUtil) + medir(tablaItems, anchoUtil)
                + medir(totales, anchoUtil);
        return Math.max(300f, contenido + (MARGEN_VERTICAL * 2) + 45f);
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
            URL logoUrl = BoletaPdfService.class.getResource("/Logo.png");
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
