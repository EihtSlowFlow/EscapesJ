package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.servicio.RentabilidadService;
import io.github.ramiro.escapesj.persistencia.PersistenceException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class VentanaRentabilidadMensual extends JFrame {
    private final RentabilidadService rentabilidadService;
    private JComboBox<Integer> comboMes;
    private JSpinner spinnerAnio;
    private JLabel lblFacturacionConCostos, lblCostoConocido, lblGanancia, lblFacturacionSinCostos, lblFacturacionTotal;
    private JLabel lblMargen, lblCantCompletas, lblCantIncompletas, lblEstadoParcial;
    
    private JTable tablaDetalles;
    private DefaultTableModel modeloTabla;
    private JComboBox<String> comboFiltroOrigen;
    
    private RentabilidadService.ResumenRentabilidad ultimoResumen;

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

        comboFiltroOrigen = new JComboBox<>(new String[]{"Todas", "Digitales", "Papel"});
        comboFiltroOrigen.addActionListener(e -> refrescarTabla());

        JButton btnCalcular = new JButton("Calcular Rentabilidad");
        btnCalcular.setBackground(new Color(52, 152, 219));
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.addActionListener(e -> calcularRentabilidad());

        pnlFiltro.add(crearLabelBlanco("Año:"));
        pnlFiltro.add(spinnerAnio);
        pnlFiltro.add(crearLabelBlanco("Mes:"));
        pnlFiltro.add(comboMes);
        pnlFiltro.add(btnCalcular);

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
            ultimoResumen = rentabilidadService.calcularResumenMensual(anio, mes);
            
            lblFacturacionTotal.setText("Facturación Total del Mes: $" + String.format("%,.2f", ultimoResumen.getFacturacionTotal()));
            lblFacturacionConCostos.setText("Facturación (Ops. Completas): $" + String.format("%,.2f", ultimoResumen.facturacionConCostos()));
            lblCostoConocido.setText("Costo de Materiales (Ops. Completas): $" + String.format("%,.2f", ultimoResumen.costoConocido()));
            lblGanancia.setText("Ganancia Calculable: $" + String.format("%,.2f", ultimoResumen.gananciaCalculable()));
            
            if (ultimoResumen.margenPorcentual() != null) {
                lblMargen.setText("Margen de Ganancia: " + String.format("%,.2f%%", ultimoResumen.margenPorcentual()));
            } else {
                lblMargen.setText("Margen de Ganancia: No disponible");
            }
            
            lblFacturacionSinCostos.setText("Facturación (Ops. Incompletas/Sin Costo): $" + String.format("%,.2f", ultimoResumen.facturacionSinCostos()));
            lblCantCompletas.setText("Cant. Operaciones Completas: " + ultimoResumen.cantidadCompletas());
            lblCantIncompletas.setText("Cant. Operaciones Incompletas: " + ultimoResumen.cantidadIncompletas());

            if (ultimoResumen.tieneResultadosParciales()) {
                lblEstadoParcial.setText("⚠️ RESULTADO PARCIAL: Hay operaciones sin costos conocidos.");
                lblEstadoParcial.setForeground(new Color(241, 196, 15));
            } else {
                lblEstadoParcial.setText("✅ RESULTADO COMPLETO");
                lblEstadoParcial.setForeground(new Color(46, 204, 113));
            }
            
            refrescarTabla();
            
        } catch (PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "calcular rentabilidad mensual", ex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error inesperado al calcular rentabilidad: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refrescarTabla() {
        modeloTabla.setRowCount(0);
        if (ultimoResumen == null) return;
        
        String filtro = (String) comboFiltroOrigen.getSelectedItem();
        List<RentabilidadService.DetalleRentabilidad> filtrados = ultimoResumen.detalles().stream()
                .filter(d -> {
                    if ("Digitales".equals(filtro)) return "Digital".equals(d.origen());
                    if ("Papel".equals(filtro)) return "Papel".equals(d.origen());
                    return true;
                })
                .collect(Collectors.toList());
                
        for (var d : filtrados) {
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
