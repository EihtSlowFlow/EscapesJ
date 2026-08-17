package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.OperacionHistorica;
import io.github.ramiro.escapesj.persistencia.OperacionHistoricaRepository;
import io.github.ramiro.escapesj.persistencia.BoletaRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;

public class VentanaOperacionesHistoricas extends JFrame {
    private final OperacionHistoricaRepository repository;
    private final BoletaRepository boletaRepository;
    private DefaultTableModel model;
    private JTable tabla;
    private JTextField txtFecha, txtRef, txtCliente, txtDesc, txtTotal, txtCosto, txtObs;

    public VentanaOperacionesHistoricas(OperacionHistoricaRepository repository, BoletaRepository boletaRepository) {
        this.repository = repository;
        this.boletaRepository = boletaRepository;
        initUI();
        actualizarTabla();
    }

    private void initUI() {
        setTitle("EscapesJ - Registros en Papel (Historial)");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91));
        setLayout(new BorderLayout(15, 15));

        // TABLA
        model = new DefaultTableModel(new Object[]{"ID", "Fecha", "Ref", "Cliente", "Descripción", "Total", "Costo", "Estado", "Vinculado", "Acción"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 9;
            }
        };

        tabla = new JTable(model);
        estilizarTabla(tabla);
        tabla.getColumnModel().getColumn(9).setCellRenderer(new ButtonRenderer());
        tabla.getColumnModel().getColumn(9).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.getViewport().setBackground(new Color(45, 52, 71));
        scrollTabla.setBorder(BorderFactory.createEmptyBorder());

        // PANEL NUEVO
        JPanel pnlNuevo = new JPanel(new GridBagLayout());
        pnlNuevo.setBackground(new Color(0, 43, 91));
        pnlNuevo.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.CYAN), "CARGAR REGISTRO", 0, 0, null, Color.CYAN));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 15, 5, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        txtFecha = crearCampo("Fecha (YYYY-MM-DD)", pnlNuevo, gbc, 0);
        txtFecha.setText(java.time.LocalDate.now().toString());
        txtRef = crearCampo("Nro Boleta Papel / Ref", pnlNuevo, gbc, 2);
        txtCliente = crearCampo("Cliente", pnlNuevo, gbc, 4);
        txtDesc = crearCampo("Descripción del trabajo", pnlNuevo, gbc, 6);
        txtTotal = crearCampo("Importe Total ($)", pnlNuevo, gbc, 8);
        txtCosto = crearCampo("Costo Materiales ($) (Vacío = No calculable)", pnlNuevo, gbc, 10);
        txtObs = crearCampo("Observaciones", pnlNuevo, gbc, 12);

        JButton btnGuardar = new JButton("Registrar");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> registrarNuevo());
        gbc.gridy = 14;
        gbc.insets = new Insets(15, 15, 10, 15);
        pnlNuevo.add(btnGuardar, gbc);

        JScrollPane scrollNuevo = new JScrollPane(pnlNuevo);
        scrollNuevo.setBorder(null);
        scrollNuevo.setPreferredSize(new Dimension(350, 0));
        scrollNuevo.getViewport().setBackground(new Color(0, 43, 91));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollTabla, scrollNuevo);
        splitPane.setBackground(new Color(0, 43, 91));
        splitPane.setBorder(null);
        splitPane.setResizeWeight(0.75);
        add(splitPane, BorderLayout.CENTER);
    }

    private void registrarNuevo() {
        try {
            String fecha = txtFecha.getText().trim();
            java.time.LocalDate.parse(fecha); // valida YYYY-MM-DD
            
            String ref = txtRef.getText().trim();
            String cliente = txtCliente.getText().trim();
            String desc = txtDesc.getText().trim();
            
            BigDecimal total = io.github.ramiro.escapesj.sdk.DineroUtil.parsearMontoArs(txtTotal.getText());

            String costoStr = txtCosto.getText().trim();
            BigDecimal costo = null;
            if (!costoStr.isEmpty()) {
                costo = io.github.ramiro.escapesj.sdk.DineroUtil.parsearMontoArs(costoStr);
            }
            
            String obs = txtObs.getText().trim();

            OperacionHistorica op = new OperacionHistorica(0, fecha, ref, cliente, desc, total, costo, obs, "PENDIENTE", null, null, null);
            repository.guardar(op);
            
            actualizarTabla();
            limpiarCampos();
            JOptionPane.showMessageDialog(this, "Registro guardado correctamente.");
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "La fecha debe tener el formato YYYY-MM-DD.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de formato", JOptionPane.ERROR_MESSAGE);
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "guardar registro", ex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage());
        }
    }

    public void actualizarTabla() {
        model.setRowCount(0);
        try {
            repository.buscarTodas().forEach(op -> {
                String costoStr = op.getCostoMateriales() == null ? "No conf." : "$" + op.getCostoMateriales();
                String vinc = "-";
                if (op.getBoletaDigitalNumero() != null) {
                    vinc = "#" + op.getBoletaDigitalNumero();
                } else if (op.getBoletaDigitalId() != null) {
                    vinc = "ID:" + op.getBoletaDigitalId();
                }
                model.addRow(new Object[]{
                    op.getId(), op.getFecha(), op.getReferenciaPapel(), op.getCliente(), 
                    op.getDescripcion(), "$" + op.getImporteTotal(), costoStr, 
                    op.getEstado(), vinc, "VER / VINCULAR"
                });
            });
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "cargar operaciones", ex);
        }
    }

    private JTextField crearCampo(String l, JPanel p, GridBagConstraints g, int y) {
        JLabel lbl = new JLabel(l);
        lbl.setForeground(Color.WHITE);
        g.gridy = y;
        p.add(lbl, g);
        JTextField f = new JTextField(15);
        f.setBackground(new Color(45, 52, 71));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        g.gridy = y + 1;
        p.add(f, g);
        return f;
    }

    private void limpiarCampos() {
        txtRef.setText("");
        txtCliente.setText("");
        txtDesc.setText("");
        txtTotal.setText("");
        txtCosto.setText("");
        txtObs.setText("");
    }

    private void estilizarTabla(JTable t) {
        t.setBackground(new Color(45, 52, 71));
        t.setForeground(Color.WHITE);
        t.setRowHeight(35);
        ZoomManager.registerBaseRowHeight(t, 35);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setBackground(new Color(30, 35, 48));
        t.getTableHeader().setForeground(Color.WHITE);
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            setText((v == null) ? "" : v.toString());
            setBackground(new Color(52, 152, 219));
            setForeground(Color.WHITE);
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> {
                int row = tabla.getSelectedRow();
                if (row >= 0) {
                    int id = (Integer) model.getValueAt(row, 0);
                    abrirDetalle(id);
                }
                fireEditingStopped();
            });
        }

        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            label = (v == null) ? "" : v.toString();
            button.setText(label);
            return button;
        }

        public Object getCellEditorValue() { return label; }
    }

    private void abrirDetalle(int id) {
        try {
            var opOpt = repository.buscarPorId(id);
            if (opOpt.isPresent()) {
                new DialogoOperacionHistorica(this, opOpt.get(), repository, boletaRepository).setVisible(true);
                actualizarTabla();
            } else {
                JOptionPane.showMessageDialog(this, "La operación histórica no existe.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "abrir detalle", ex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
