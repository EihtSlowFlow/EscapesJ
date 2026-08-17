package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.servicio.RentabilidadService;
import io.github.ramiro.escapesj.persistencia.PersistenceException;
import io.github.ramiro.escapesj.servicio.RentabilidadService.FiltroOrigen;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class VentanaRentabilidadMensual extends JFrame {
    private final RentabilidadService rentabilidadService;
    private JComboBox<Integer> comboMes;
    private JSpinner spinnerAnio;
    private JLabel lblFacturacionConCostos, lblCostoConocido, lblGanancia, lblFacturacionSinCostos, lblFacturacionTotal;
    private JLabel lblMargen, lblCantCompletas, lblCantIncompletas, lblEstadoParcial;
    
    private JTable tablaDetalles;
    private DefaultTableModel modeloTabla;
    private JComboBox<FiltroOrigen> comboFiltroOrigen;
    private JButton btnExportarPdf;
    
    private RentabilidadService.ResumenRentabilidad resumenCompleto;
    private RentabilidadService.ResumenRentabilidad resumenVisible;

    public VentanaRentabilidadMensual(RentabilidadService rentabilidadService) {
        this.rentabilidadService = rentabilidadService;
        initUI();
    }

    private void initUI() {
        setTitle("Panel de Rentabilidad Mensual");
        getContentPane().setBackground(new Color(0, 43, 91));
        setLayout(new BorderLayout(15, 15));

        JPanel pnlFiltro = new JPanel(new FlowLayout());
        pnlFiltro.setBackground(new Color(30, 35, 48));
        
        int currentYear = LocalDate.now().getYear();
        SpinnerModel yearModel = new SpinnerNumberModel(currentYear, null, null, 1);
        spinnerAnio = new JSpinner(yearModel);
        spinnerAnio.setEditor(new JSpinner.NumberEditor(spinnerAnio, "#"));
        
        Integer[] meses = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        comboMes = new JComboBox<>(meses);
        comboMes.setSelectedItem(LocalDate.now().getMonthValue());

        comboFiltroOrigen = new JComboBox<>(FiltroOrigen.values());
        comboFiltroOrigen.addActionListener(e -> {
            if (resumenCompleto != null) {
                aplicarFiltroYActualizarVista();
            }
        });

        JButton btnCalcular = new JButton("Calcular Rentabilidad");
        btnCalcular.setBackground(new Color(52, 152, 219));
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.addActionListener(e -> calcularRentabilidad());

        btnExportarPdf = new JButton("Exportar PDF");
        btnExportarPdf.setBackground(new Color(46, 204, 113));
        btnExportarPdf.setForeground(Color.WHITE);
        btnExportarPdf.setEnabled(false);
        btnExportarPdf.addActionListener(e -> exportarAPdf());

        pnlFiltro.add(crearLabelBlanco("Año:"));
        pnlFiltro.add(spinnerAnio);
        pnlFiltro.add(crearLabelBlanco("Mes:"));
        pnlFiltro.add(comboMes);
        pnlFiltro.add(btnCalcular);
        pnlFiltro.add(btnExportarPdf);

        add(pnlFiltro, BorderLayout.NORTH);

        JPanel pnlCentro = new JPanel(new BorderLayout(10, 10));
        pnlCentro.setBackground(new Color(0, 43, 91));
        
        JPanel pnlResultados = new JPanel(new GridLayout(10, 1, 10, 10));
        pnlResultados.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        pnlResultados.setBackground(new Color(0, 43, 91));

        lblFacturacionTotal = crearLabelRes("");
        lblFacturacionConCostos = crearLabelRes("");
        lblCostoConocido = crearLabelRes("");
        lblGanancia = crearLabelRes("");
        lblMargen = crearLabelRes("");
        lblFacturacionSinCostos = crearLabelRes("");
        lblCantCompletas = crearLabelRes("");
        lblCantIncompletas = crearLabelRes("");
        lblEstadoParcial = new JLabel("");
        lblEstadoParcial.setForeground(new Color(241, 196, 15));
        lblEstadoParcial.setFont(new Font("SansSerif", Font.BOLD, 14));

        pnlResultados.add(lblEstadoParcial);
        pnlResultados.add(lblFacturacionTotal);
        pnlResultados.add(lblFacturacionConCostos);
        pnlResultados.add(lblCostoConocido);
        pnlResultados.add(lblGanancia);
        pnlResultados.add(lblMargen);
        pnlResultados.add(new JSeparator());
        pnlResultados.add(lblFacturacionSinCostos);
        pnlResultados.add(lblCantCompletas);
        pnlResultados.add(lblCantIncompletas);

        pnlCentro.add(pnlResultados, BorderLayout.NORTH);

        // Tabla
        modeloTabla = new DefaultTableModel(new Object[]{"Origen", "Fecha", "Cliente", "Facturación", "Ganancia", "Margen", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaDetalles = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDetalles);
        scrollTabla.getViewport().setBackground(new Color(0, 43, 91));
        
        JPanel pnlTabla = new JPanel(new BorderLayout(5, 5));
        pnlTabla.setBackground(new Color(0, 43, 91));
        pnlTabla.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        
        JPanel pnlFiltroTabla = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlFiltroTabla.setBackground(new Color(0, 43, 91));
        pnlFiltroTabla.add(crearLabelBlanco("Filtrar por origen:"));
        pnlFiltroTabla.add(comboFiltroOrigen);
        
        pnlTabla.add(pnlFiltroTabla, BorderLayout.NORTH);
        pnlTabla.add(scrollTabla, BorderLayout.CENTER);

        pnlCentro.add(pnlTabla, BorderLayout.CENTER);

        add(pnlCentro, BorderLayout.CENTER);
        
        ZoomManager.packAndFitToScreen(this, 1000, 700);
    }

    private void calcularRentabilidad() {
        int anio = (Integer) spinnerAnio.getValue();
        int mes = (Integer) comboMes.getSelectedItem();

        try {
            resumenCompleto = rentabilidadService.calcularResumenMensual(anio, mes);
            aplicarFiltroYActualizarVista();
            btnExportarPdf.setEnabled(true);
        } catch (PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "calcular rentabilidad mensual", ex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error inesperado al calcular rentabilidad: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarFiltroYActualizarVista() {
        FiltroOrigen origen = (FiltroOrigen) comboFiltroOrigen.getSelectedItem();
        resumenVisible = rentabilidadService.filtrarPorOrigen(resumenCompleto, origen);
        
        lblFacturacionTotal.setText("Facturación Total del Mes: $" + String.format("%,.2f", resumenVisible.getFacturacionTotal()));
        lblFacturacionConCostos.setText("Facturación (Ops. Completas): $" + String.format("%,.2f", resumenVisible.facturacionConCostos()));
        lblCostoConocido.setText("Costo de Materiales (Ops. Completas): $" + String.format("%,.2f", resumenVisible.costoConocido()));
        lblGanancia.setText("Ganancia bruta calculable: $" + String.format("%,.2f", resumenVisible.gananciaCalculable()));
        
        if (resumenVisible.margenPorcentual() != null) {
            lblMargen.setText("Margen de Ganancia: " + String.format("%,.2f%%", resumenVisible.margenPorcentual()));
        } else {
            lblMargen.setText("Margen de Ganancia: No disponible");
        }
        
        lblFacturacionSinCostos.setText("Facturación (Ops. Incompletas/Sin Costo): $" + String.format("%,.2f", resumenVisible.facturacionSinCostos()));
        lblCantCompletas.setText("Cant. Operaciones Completas: " + resumenVisible.cantidadCompletas());
        lblCantIncompletas.setText("Cant. Operaciones Incompletas: " + resumenVisible.cantidadIncompletas());

        if (resumenVisible.tieneResultadosParciales()) {
            lblEstadoParcial.setText("⚠️ RESULTADO PARCIAL: Hay operaciones sin costos conocidos.");
            lblEstadoParcial.setForeground(new Color(241, 196, 15));
        } else {
            lblEstadoParcial.setText("✅ RESULTADO COMPLETO");
            lblEstadoParcial.setForeground(new Color(46, 204, 113));
        }
        
        refrescarTabla();
    }
    
    private void exportarAPdf() {
        if (resumenVisible == null) return;

        int anio = (Integer) spinnerAnio.getValue();
        int mes = (Integer) comboMes.getSelectedItem();
        FiltroOrigen filtro = (FiltroOrigen) comboFiltroOrigen.getSelectedItem();

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Informe PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        chooser.setSelectedFile(new File(String.format("Rentabilidad-%04d-%02d.pdf", anio, mes)));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getParentFile(), file.getName() + ".pdf");
            }

            if (file.exists()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "El archivo ya existe. ¿Desea sobrescribirlo?",
                        "Confirmar reemplazo",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                io.github.ramiro.escapesj.servicio.RentabilidadPdfService.generarPdf(resumenVisible, anio, mes, filtro, file);
                
                int open = JOptionPane.showConfirmDialog(this,
                        "El PDF se generó exitosamente.\n¿Desea abrirlo ahora?",
                        "Exportación exitosa",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                        
                if (open == JOptionPane.YES_OPTION) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(file);
                    } else {
                        JOptionPane.showMessageDialog(this, "La acción de abrir archivos no está soportada en este sistema.\nEl archivo se encuentra en: " + file.getAbsolutePath());
                    }
                }
            } catch (Exception ex) {
                // Not a persistence exception, so we show a friendly message and log it
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Hubo un problema al crear o guardar el PDF.\n" + ex.getMessage(),
                        "Error de exportación",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void refrescarTabla() {
        modeloTabla.setRowCount(0);
        if (resumenVisible == null) return;
        
        for (var d : resumenVisible.detalles()) {
            String gananciaStr = d.completa() && d.ganancia() != null ? String.format("$%,.2f", d.ganancia()) : "N/A";
            String margenStr = d.completa() && d.margen() != null ? String.format("%,.2f%%", d.margen()) : "N/A";
            
            modeloTabla.addRow(new Object[]{
                d.origen(),
                d.fecha(),
                d.cliente(),
                String.format("$%,.2f", d.facturacion()),
                gananciaStr,
                margenStr,
                d.completa() ? "Completa" : "Incompleta"
            });
        }
    }

    private JLabel crearLabelBlanco(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        return l;
    }

    private JLabel crearLabelRes(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif", Font.PLAIN, 16));
        return l;
    }
}
