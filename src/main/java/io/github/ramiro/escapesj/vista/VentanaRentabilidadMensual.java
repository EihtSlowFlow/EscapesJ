package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.servicio.RentabilidadService;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class VentanaRentabilidadMensual extends JFrame {
    private final RentabilidadService rentabilidadService;
    private JComboBox<Integer> comboMes;
    private JComboBox<Integer> comboAnio;
    private JLabel lblFacturacionConCostos, lblCostoConocido, lblGanancia, lblFacturacionSinCostos;
    private JLabel lblCantCompletas, lblCantIncompletas, lblEstadoParcial;

    public VentanaRentabilidadMensual(RentabilidadService rentabilidadService) {
        this.rentabilidadService = rentabilidadService;
        initUI();
    }

    private void initUI() {
        setTitle("Panel de Rentabilidad Mensual");
        setSize(500, 600);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91));
        setLayout(new BorderLayout(15, 15));

        JPanel pnlFiltro = new JPanel(new FlowLayout());
        pnlFiltro.setBackground(new Color(30, 35, 48));
        
        int currentYear = LocalDate.now().getYear();
        Integer[] anios = new Integer[currentYear + 5 - 2020 + 1];
        for (int i = 0; i < anios.length; i++) {
            anios[i] = 2020 + i;
        }
        comboAnio = new JComboBox<>(anios);
        comboAnio.setSelectedItem(currentYear);
        
        Integer[] meses = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        comboMes = new JComboBox<>(meses);
        comboMes.setSelectedItem(LocalDate.now().getMonthValue());

        JButton btnCalcular = new JButton("Calcular Rentabilidad");
        btnCalcular.setBackground(new Color(52, 152, 219));
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.addActionListener(e -> calcularRentabilidad());

        pnlFiltro.add(crearLabelBlanco("Año:"));
        pnlFiltro.add(comboAnio);
        pnlFiltro.add(crearLabelBlanco("Mes:"));
        pnlFiltro.add(comboMes);
        pnlFiltro.add(btnCalcular);

        add(pnlFiltro, BorderLayout.NORTH);

        JPanel pnlResultados = new JPanel(new GridLayout(8, 1, 10, 10));
        pnlResultados.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        pnlResultados.setBackground(new Color(0, 43, 91));

        lblFacturacionConCostos = crearLabelRes("");
        lblCostoConocido = crearLabelRes("");
        lblGanancia = crearLabelRes("");
        lblFacturacionSinCostos = crearLabelRes("");
        lblCantCompletas = crearLabelRes("");
        lblCantIncompletas = crearLabelRes("");
        lblEstadoParcial = new JLabel("");
        lblEstadoParcial.setForeground(new Color(241, 196, 15));
        lblEstadoParcial.setFont(new Font("SansSerif", Font.BOLD, 14));

        pnlResultados.add(lblEstadoParcial);
        pnlResultados.add(lblFacturacionConCostos);
        pnlResultados.add(lblCostoConocido);
        pnlResultados.add(lblGanancia);
        pnlResultados.add(new JSeparator());
        pnlResultados.add(lblFacturacionSinCostos);
        pnlResultados.add(lblCantCompletas);
        pnlResultados.add(lblCantIncompletas);

        add(pnlResultados, BorderLayout.CENTER);
    }

    private void calcularRentabilidad() {
        int anio = (Integer) comboAnio.getSelectedItem();
        int mes = (Integer) comboMes.getSelectedItem();

        try {
            var resumen = rentabilidadService.calcularResumenMensual(anio, mes);
            
            lblFacturacionConCostos.setText("Facturación (Ops. Completas): $" + String.format("%,.2f", resumen.facturacionConCostos()));
            lblCostoConocido.setText("Costo de Materiales (Ops. Completas): $" + String.format("%,.2f", resumen.costoConocido()));
            lblGanancia.setText("Ganancia Calculable: $" + String.format("%,.2f", resumen.gananciaCalculable()));
            
            lblFacturacionSinCostos.setText("Facturación (Ops. Incompletas/Sin Costo): $" + String.format("%,.2f", resumen.facturacionSinCostos()));
            lblCantCompletas.setText("Cant. Operaciones Completas: " + resumen.cantidadCompletas());
            lblCantIncompletas.setText("Cant. Operaciones Incompletas: " + resumen.cantidadIncompletas());

            if (resumen.tieneResultadosParciales()) {
                lblEstadoParcial.setText("⚠️ RESULTADO PARCIAL: Hay operaciones sin costos conocidos.");
                lblEstadoParcial.setForeground(new Color(241, 196, 15));
            } else {
                lblEstadoParcial.setText("✅ RESULTADO COMPLETO");
                lblEstadoParcial.setForeground(new Color(46, 204, 113));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al calcular rentabilidad: " + ex.getMessage());
            ex.printStackTrace();
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
