package io.github.ramiro.escapesj.servicio;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import io.github.ramiro.escapesj.servicio.RentabilidadService.ResumenRentabilidad;
import io.github.ramiro.escapesj.servicio.RentabilidadService.DetalleRentabilidad;
import io.github.ramiro.escapesj.servicio.RentabilidadService.FiltroOrigen;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RentabilidadPdfService {

    public static void generarPdf(ResumenRentabilidad resumen, int anio, int mes, FiltroOrigen filtroOrigen, File archivoDestino) throws Exception {
        Path destinoPath = archivoDestino.toPath();
        Path tempPath = Files.createTempFile(destinoPath.getParent(), ".rentabilidad-", ".tmp");

        try {
            try (FileOutputStream fos = new FileOutputStream(tempPath.toFile())) {
                Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
                PdfWriter writer = PdfWriter.getInstance(document, fos);
                writer.setPageEvent(new NumeracionPaginas());

                try {
                
                document.open();

                Font fTitle = new Font(Font.HELVETICA, 16, Font.BOLD);
                Font fSubtitle = new Font(Font.HELVETICA, 12, Font.BOLD);
                Font fBody = new Font(Font.HELVETICA, 10, Font.NORMAL);
                Font fBodyBold = new Font(Font.HELVETICA, 10, Font.BOLD);
                Font fGanancia = new Font(Font.HELVETICA, 10, Font.BOLD, new java.awt.Color(46, 204, 113));
                Font fPerdida = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.RED);
                Font fWarning = new Font(Font.HELVETICA, 10, Font.BOLD, new java.awt.Color(241, 196, 15));

                // Título y datos generales
                Paragraph pTitle = new Paragraph("Informe de Rentabilidad Mensual", fTitle);
                pTitle.setAlignment(Element.ALIGN_CENTER);
                document.add(pTitle);

                String mesAnioStr = String.format("%02d/%04d", mes, anio);
                document.add(new Paragraph("Período analizado: " + mesAnioStr, fBody));
                document.add(new Paragraph("Filtro aplicado: " + filtroOrigen.toString(), fBody));
                document.add(new Paragraph("Fecha de generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fBody));
                document.add(new Paragraph(" "));

                java.util.Locale ar = java.util.Locale.forLanguageTag("es-AR");

                // Resumen de métricas
                document.add(new Paragraph("Resumen de Métricas", fSubtitle));
                document.add(new Paragraph(String.format(ar, "Facturación Total del Mes: $%,.2f", resumen.getFacturacionTotal()), fBodyBold));
                document.add(new Paragraph(String.format(ar, "Facturación (Ops. Completas): $%,.2f", resumen.facturacionConCostos()), fBody));
                document.add(new Paragraph(String.format(ar, "Costo conocido de materiales: $%,.2f", resumen.costoConocido()), fBody));
                
                Paragraph pGanancia;
                if (resumen.gananciaCalculable().compareTo(java.math.BigDecimal.ZERO) < 0) {
                    pGanancia = new Paragraph(String.format(ar, "Ganancia bruta calculable: $%,.2f", resumen.gananciaCalculable()), fPerdida);
                } else {
                    pGanancia = new Paragraph(String.format(ar, "Ganancia bruta calculable: $%,.2f", resumen.gananciaCalculable()), fGanancia);
                }
                document.add(pGanancia);
                
                String margenStr = resumen.margenPorcentual() != null ? String.format(ar, "%,.2f%%", resumen.margenPorcentual()) : "No disponible";
                document.add(new Paragraph("Margen de ganancia: " + margenStr, fBody));
                
                document.add(new Paragraph(String.format(ar, "Facturación (Ops. Incompletas/Sin costo): $%,.2f", resumen.facturacionSinCostos()), fBody));
                document.add(new Paragraph("Cantidad Operaciones Completas: " + resumen.cantidadCompletas(), fBody));
                document.add(new Paragraph("Cantidad Operaciones Incompletas: " + resumen.cantidadIncompletas(), fBody));

                if (resumen.tieneResultadosParciales()) {
                    document.add(new Paragraph("ADVERTENCIA - RESULTADO PARCIAL: Hay operaciones sin costos conocidos.", fWarning));
                }

                document.add(new Paragraph(" "));
                
                // Tabla detallada
                document.add(new Paragraph("Detalle de Operaciones", fSubtitle));
                document.add(new Paragraph(" "));

                PdfPTable tabla = new PdfPTable(8);
                tabla.setWidthPercentage(100);
                tabla.setWidths(new float[]{1.2f, 1.2f, 3f, 1.8f, 1.5f, 1.8f, 1.5f, 1.5f});
                tabla.setHeaderRows(1);

                // Headers
                String[] headers = {"Origen", "Fecha", "Cliente", "Facturación", "Costo", "Ganancia bruta", "Margen", "Estado"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Paragraph(h, fBodyBold));
                    cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                    cell.setPadding(5);
                    tabla.addCell(cell);
                }

                // Datos
                for (DetalleRentabilidad d : resumen.detalles()) {
                    tabla.addCell(crearCelda(d.origen(), fBody));
                    tabla.addCell(crearCelda(io.github.ramiro.escapesj.sdk.DateUtil.formatoLocal(d.fecha()), fBody));
                    tabla.addCell(crearCelda(d.cliente(), fBody));
                    tabla.addCell(crearCelda(String.format(ar, "$%,.2f", d.facturacion()), fBody));

                    if (d.completa()) {
                        tabla.addCell(crearCelda(String.format(ar, "$%,.2f", d.costo()), fBody));
                        
                        Font fontGanancia = d.ganancia().compareTo(java.math.BigDecimal.ZERO) < 0 ? fPerdida : fGanancia;
                        tabla.addCell(crearCelda(String.format(ar, "$%,.2f", d.ganancia()), fontGanancia));
                        
                        tabla.addCell(crearCelda(String.format(ar, "%,.2f%%", d.margen()), fBody));
                        tabla.addCell(crearCelda("Completa", fBody));
                    } else {
                        tabla.addCell(crearCelda("No disponible", fWarning));
                        tabla.addCell(crearCelda("No calculable", fWarning));
                        tabla.addCell(crearCelda("No calculable", fWarning));
                        tabla.addCell(crearCelda("Incompleta", fWarning));
                    }
                }

                    document.add(tabla);
                } finally {
                    if (document.isOpen()) {
                        document.close();
                    }
                }
            }

            // El stream ya está completamente cerrado antes de mover, requisito en Windows.
            try {
                Files.move(tempPath, destinoPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempPath, destinoPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
        } catch (Exception e) {
            // Eliminar temporal si falla la generación o el movimiento
            try {
                Files.deleteIfExists(tempPath);
            } catch (Exception ignored) {}
            
            throw new Exception("Error al generar o guardar el informe PDF: " + e.getMessage(), e);
        }
    }

    private static PdfPCell crearCelda(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(texto, font));
        cell.setPadding(5);
        return cell;
    }

    private static final class NumeracionPaginas extends PdfPageEventHelper {
        private static final Font FONT = new Font(Font.HELVETICA, 8);
        private static final BaseFont BASE_FONT = FONT.getCalculatedBaseFont(false);
        private PdfTemplate totalPaginas;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPaginas = writer.getDirectContent().createTemplate(20, 10);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            String prefijo = "Página " + writer.getPageNumber() + " de ";
            float anchoPrefijo = BASE_FONT.getWidthPoint(prefijo, FONT.getSize());
            float x = document.right() - anchoPrefijo - 20;
            float y = document.bottom() - 15;
            PdfContentByte contenido = writer.getDirectContent();

            contenido.beginText();
            contenido.setFontAndSize(BASE_FONT, FONT.getSize());
            contenido.setTextMatrix(x, y);
            contenido.showText(prefijo);
            contenido.endText();
            contenido.addTemplate(totalPaginas, x + anchoPrefijo, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPaginas.beginText();
            totalPaginas.setFontAndSize(BASE_FONT, FONT.getSize());
            totalPaginas.setTextMatrix(0, 0);
            totalPaginas.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPaginas.endText();
        }
    }
}
