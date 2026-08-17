package io.github.ramiro.escapesj.vista;

import io.github.ramiro.escapesj.modelo.Producto;
import io.github.ramiro.escapesj.persistencia.ProductoRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;

public class VentanaGestionInventario extends JFrame {
    private final ProductoRepository repository;
    private DefaultTableModel model;
    private JTable tabla;
    private JTextField txtCod, txtNom, txtDesc, txtPre, txtCosto, txtStock;

    public VentanaGestionInventario(ProductoRepository repository) {
        this.repository = repository;
        initUI();
        actualizarTabla();
    }

    private void initUI() {
        setTitle("EscapesJ - Inventario (Modo Separado)");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0, 43, 91));
        setLayout(new BorderLayout(15, 15));

        // 1. TABLA CON BOTÓN "MODIFICAR"
        model = new DefaultTableModel(new Object[]{"Código", "Nombre", "Descripción", "Precio", "Costo", "Stock", "Acción"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            } // Solo la columna del botón es editable
        };

        tabla = new JTable(model);
        estilizarTabla(tabla);

        // Configurar el botón dentro de la celda
        tabla.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        tabla.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.getViewport().setBackground(new Color(45, 52, 71));
        scrollTabla.setBackground(new Color(45, 52, 71));
        scrollTabla.setOpaque(true);
        scrollTabla.getViewport().setOpaque(true);
        scrollTabla.setBorder(BorderFactory.createEmptyBorder());
        // 2. PANEL DERECHO: EXCLUSIVO PARA NUEVOS
        JPanel pnlNuevo = new JPanel(new GridBagLayout());
        pnlNuevo.setOpaque(true);
        pnlNuevo.setBackground(new Color(0, 43, 91));
        pnlNuevo.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.CYAN), "CARGAR NUEVO PRODUCTO", 0, 0, null, Color.CYAN));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        txtCod = crearCampo("Código Nuevo", pnlNuevo, gbc, 0);
        txtNom = crearCampo("Nombre / Modelo", pnlNuevo, gbc, 2);
        txtDesc = crearCampo("Descripción Técnica", pnlNuevo, gbc, 4);
        txtPre = crearCampo("Precio Unitario", pnlNuevo, gbc, 6);
        txtCosto = crearCampo("Costo Unitario (Vacío = Desc.)", pnlNuevo, gbc, 8);
        txtStock = crearCampo("Stock Inicial", pnlNuevo, gbc, 10);

        JButton btnGuardar = new JButton("Registrar Producto");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> registrarNuevo());
        gbc.gridy = 12;
        pnlNuevo.add(btnGuardar, gbc);

        JScrollPane scrollNuevo = new JScrollPane(pnlNuevo);
        scrollNuevo.setOpaque(true);
        scrollNuevo.setBackground(new Color(0, 43, 91));
        scrollNuevo.getViewport().setOpaque(true);
        scrollNuevo.getViewport().setBackground(new Color(0, 43, 91));
        scrollNuevo.setBorder(null);
        scrollNuevo.setPreferredSize(new Dimension(350, 0));
        scrollNuevo.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollTabla, scrollNuevo);
        splitPane.setBackground(new Color(0, 43, 91));
        splitPane.setBorder(null);
        splitPane.setResizeWeight(0.75);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.75));
        add(splitPane, BorderLayout.CENTER);
    }

    private void registrarNuevo() {
        try {
            java.math.BigDecimal precio = io.github.ramiro.escapesj.sdk.DineroUtil.parsearMontoArs(txtPre.getText());
            if (precio.compareTo(java.math.BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El precio no puede ser negativo.");
            
            String costoStr = txtCosto.getText().trim();
            java.math.BigDecimal costo = null;
            if (!costoStr.isEmpty()) {
                costo = io.github.ramiro.escapesj.sdk.DineroUtil.parsearMontoArs(costoStr);
                if (costo.compareTo(java.math.BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El costo no puede ser negativo.");
            }
            int stock = Integer.parseInt(txtStock.getText().trim());
            if (stock < 0) throw new IllegalArgumentException("El stock no puede ser negativo.");

            Producto p = new Producto(txtCod.getText().trim(), txtNom.getText().trim(),
                    txtDesc.getText().trim(), precio, stock, costo);
            repository.guardar(p);
            actualizarTabla();
            limpiarCampos();
            JOptionPane.showMessageDialog(this, "Producto registrado con éxito.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Verifique los formatos de precio y stock.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "registrar producto", ex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error en los datos.");
        }
    }

    private void actualizarTabla() {
        model.setRowCount(0);
        try {
            repository.buscarTodos().forEach(p -> {
                String costoStr = p.getCostoUnitario() == null ? "No conf." : "$" + p.getCostoUnitario();
                model.addRow(new Object[]{p.getCodigo(), p.getNombre(), p.getDescripcion(), "$" + p.getPrecio(), costoStr, p.getStock(), "MODIFICAR"});
            });
        } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
            ErrorHandler.mostrarErrorPersistencia(this, "cargar inventario", ex);
        }
    }

    // --- RENDERER Y EDITOR DEL BOTÓN DENTRO DE LA TABLA ---

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

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
                String codigo = model.getValueAt(row, 0).toString();
                try {
                    repository.buscarPorCodigo(codigo).ifPresent(p -> {
                        VentanaModificarProducto dialog = new VentanaModificarProducto(VentanaGestionInventario.this, p, repository);
                        dialog.setVisible(true);
                        if (dialog.isActualizado()) actualizarTabla();
                    });
                } catch (io.github.ramiro.escapesj.persistencia.PersistenceException ex) {
                    ErrorHandler.mostrarErrorPersistencia(VentanaGestionInventario.this, "cargar producto a modificar", ex);
                }
                fireEditingStopped();
            });
        }

        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            label = (v == null) ? "" : v.toString();
            button.setText(label);
            return button;
        }

        public Object getCellEditorValue() {
            return label;
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
        txtCod.setText("");
        txtNom.setText("");
        txtDesc.setText("");
        txtPre.setText("");
        txtCosto.setText("");
        txtStock.setText("");
    }

    private void estilizarTabla(JTable t) {
        t.setBackground(new Color(45, 52, 71));
        t.setForeground(Color.WHITE);
        t.setRowHeight(35);
        ZoomManager.registerBaseRowHeight(t, 35);
        t.setFillsViewportHeight(true);
        t.setOpaque(true);
        t.getTableHeader().setBackground(new Color(30, 35, 48));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setOpaque(true);
    }
}
